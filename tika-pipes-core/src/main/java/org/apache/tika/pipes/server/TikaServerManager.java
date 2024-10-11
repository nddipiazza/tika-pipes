package org.apache.tika.pipes.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.tika.pipes.reaper.TempFileReaperService;
import org.apache.tika.pipes.util.OSUtils;

public class TikaServerManager implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(TikaServerManager.class);
  public static final String FUSION_HOME = System.getProperty("apollo.home");
  public static final String APP_WORK_DIR = System.getProperty("apollo.app.workDir", System.getProperty("java.io.tmpdir"));
  private static final AtomicLong TIKA_SERVER_RR = new AtomicLong(0L);
  public static final String TIKA_SERVER_MANAGER_SYSPROP = TikaServerConstants.DFUSION_MANAGER_ID + UUID.randomUUID().toString();
  private int tikaServerManagerPort = TikaServerConstants.TIKA_SERVER_MANAGER_PORT;
  private String tikaServerManagerSysProp = TIKA_SERVER_MANAGER_SYSPROP;

  private TempFileReaperService tempFileReaperService;

  private int numProcesses = TikaServerConstants.NUM_PROCESSES;
  private int maxHeapSize = TikaServerConstants.MAX_HEAP_MB;
  private long idleTikaProcessReaperMaxIdleMs = TikaServerConstants.IDLE_TIKA_PROCESS_REAPER_MAX_IDLE_MS;
  private long idleTikaProcessReaperInitialDelay = TikaServerConstants.IDLE_TIKA_PROCESS_REAPER_INITIAL_DELAY;
  private long idleTikaProcessReaperDelay = TikaServerConstants.IDLE_TIKA_PROCESS_REAPER_DELAY;
  private TimeUnit idleTikaProcessReaperDelayUnit = TikaServerConstants.IDLE_TIKA_PROCESS_REAPER_DELAY_UNIT;
  private boolean allowLocalFileOptimization = TikaServerConstants.ALLOW_LOCAL_FILE_OPTIMIZATION;

  private final List<TikaServer> servers = Lists.newArrayList();

  private String workDirectoryPath;
  private String tikaServerJar;

  public TikaServerManager() {
    if (StringUtils.isBlank(FUSION_HOME)) {
      throw new RuntimeException("The system environment variable apollo.home must be specified to use the default constructor.");
    }
    Path fatJar = Paths.get(FUSION_HOME, "apps", "tika-server", "tika-server.jar");
    this.tikaServerJar = fatJar.toAbsolutePath().toString();
    checkUberJarExists(tikaServerJar);
    this.workDirectoryPath = Paths.get(APP_WORK_DIR).toAbsolutePath().toString();
  }

  public TikaServerManager(String tikaServerJar) {
    checkUberJarExists(tikaServerJar);
    this.tikaServerJar = tikaServerJar;
    this.workDirectoryPath = Paths.get(APP_WORK_DIR).toAbsolutePath().toString();
  }

  public TikaServerManager(String tikaServerJar, String workDirectoryPath) {
    checkUberJarExists(tikaServerJar);
    this.tikaServerJar = tikaServerJar;
    this.workDirectoryPath = workDirectoryPath;
  }

  private void checkUberJarExists(String tikaServerJar) {
    if (!Files.exists(Paths.get(tikaServerJar))) {
      throw new RuntimeException("Tika Server Uberjar is not found: " + tikaServerJar);
    }
  }

  public void init() {
    killOrphanedTikaProcesses();

    log.info("Tika server uberjar path: {}", tikaServerJar);
    log.info("Tika server workDir path: {}", workDirectoryPath);
    new File(workDirectoryPath).mkdirs();
    for (int i = 0; i < numProcesses; ++i) {
      TikaServer server = new TikaServer(tikaServerJar,
          workDirectoryPath,
          maxHeapSize,
          TikaServerConstants.TIKA_SERVER_INHERIT_SERVER_LOGS,
          TikaServerConstants.PING_TIMEOUT_MILLIS,
          TikaServerConstants.PING_PULSE_MILLIS,
          TikaServerConstants.TASK_TIMEOUT_MILLIS,
          TikaServerConstants.TASK_PULSE_MILLIS,
          TikaServerConstants.CHILD_STARTUP_MILLIS,
          allowLocalFileOptimization,
          tikaServerManagerSysProp);
      servers.add(server);
    }
    for (TikaServer remote : servers) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          remote.close();
        } catch (Exception e) {
          log.debug("Error stopping TikaServer", e);
        }
      }));
    }
    tempFileReaperService = new TempFileReaperService(Collections.singletonList(workDirectoryPath));
  }

  /**
   * Kill any already existing tika services that belong to this fusion service. This helps situation where
   * people force kill connectors, api, etc process. It can leave orphaned task until this.
   */
  private void killOrphanedTikaProcesses() {
    Set<Integer> tikaProcessIds = OSUtils.getJavaProcessIds(StringUtils.split(tikaServerManagerSysProp, "=")[0]);
    tikaProcessIds.forEach(pid -> {
      log.warn("Killing orphaned tika that was previously started by Fusion - pid={}", pid);
      OSUtils.forceKillPid(pid);
    });
  }

  public TikaServer getTikaServer(int idx) {
    return servers.get(idx);
  }

  /**
   * Gets a randomly load balanced tika server from the server manager.
   *
   * @return Tika endpoint.
   */
  public String getRandomTikaServerFromAvailableServers() throws Exception {
    int nextIndex = (int) (TIKA_SERVER_RR.incrementAndGet() % numProcesses);
    return getTikaServer(nextIndex).getTikaEndpoint();
  }

  /**
   * Gets a randomly load balanced tika server from the server manager.
   *
   * @return Tika endpoint.
   */
  public List<String> getTikaServerEndpoints() {
    return servers.stream().map(TikaServer::getTikaEndpoint).collect(Collectors.toList());
  }

  @Override
  public void close() {
    servers.forEach(TikaServer::close);
    tempFileReaperService.close();
  }

  private boolean isAllPortsIp(String host) {
    return StringUtils.isBlank(host) || "0.0.0.0".equals(host);
  }

  private void handleEndpointRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int nextIndex = (int) (TIKA_SERVER_RR.incrementAndGet() % numProcesses);
    TikaServer tikaServer = servers.get(nextIndex);
    response.setContentType("text/plain");
    String endpoint = String.format("http://%s:%d", isAllPortsIp(tikaServer.getHost()) ? request.getLocalAddr() : tikaServer.getHost(), tikaServer.getPort());
    response.getOutputStream().write(endpoint.getBytes());
  }

  private void handleStatusRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
    List<Map> statuses = new ArrayList<>();
    for (TikaServer tikaServer : servers) {
      Map<String, Object> tikaServerStatus = Maps.newHashMap();
      if (StringUtils.isNotBlank(tikaServer.getHost())) {
        tikaServerStatus.put("boundToHost", tikaServer.getHost());
      }
      String endpoint = String.format("http://%s:%d", isAllPortsIp(tikaServer.getHost()) ? request.getLocalAddr() : tikaServer.getHost(), tikaServer.getPort());
      tikaServerStatus.put("endpoint", endpoint);
      tikaServerStatus.put("running", tikaServer.isRunning());
      if (tikaServer.isRunning()) {
        Map status = TikaServer.getTikaServerStatus(tikaServer.getHost(), tikaServer.getPort());
        tikaServerStatus.put("status", status);
      }
      statuses.add(tikaServerStatus);
    }
    response.addHeader("Content-Type", "application/json");
    response.getOutputStream().print(new Gson().toJson(statuses));
  }

  public void run(boolean join) throws Exception {
    AtomicInteger serverNum = new AtomicInteger(0);
    servers.forEach(tikaServer -> {
      try {
        log.info("Starting tika server {}", serverNum.incrementAndGet());
        tikaServer.startServer();
      } catch (IOException e) {
        throw new RuntimeException("Could not start initial tika server", e);
      }
    });

    // on jvm shutdown, kill the tika servers gracefully.
    Runtime.getRuntime().addShutdownHook(new Thread(this::close));

    log.info("Starting the tika server manager on port {}", tikaServerManagerPort);

    Server server = new Server(tikaServerManagerPort);
    Thread serverThread = new Thread(() -> server.setHandler(new AbstractHandler() {
      @Override
      public void handle(String target,
                         Request baseRequest,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException, ServletException {
        String action = request.getParameter("action");
        if ("endpoint".equalsIgnoreCase(action)) {
          handleEndpointRequest(request, response);
        } else if ("status".equalsIgnoreCase(action)) {
          handleStatusRequest(request, response);
        }
        baseRequest.setHandled(true);
        response.setStatus(200);
      }
    }));

    serverThread.start();
    log.info("Successfully started the tika server manager on port {}", tikaServerManagerPort);
    server.start();
    if (join) {
      server.join();
    }
  }

  public int getNumProcesses() {
    return numProcesses;
  }

  public TikaServerManager setNumProcesses(int numProcesses) {
    this.numProcesses = numProcesses;
    return this;
  }

  public int getMaxHeapSize() {
    return maxHeapSize;
  }

  public TikaServerManager setMaxHeapSize(int maxHeapSize) {
    this.maxHeapSize = maxHeapSize;
    return this;
  }

  public int getTikaServerManagerPort() {
    return tikaServerManagerPort;
  }

  public void setTikaServerManagerPort(int tikaServerManagerPort) {
    this.tikaServerManagerPort = tikaServerManagerPort;
  }

  public void setTikaServerManagerSysProp(String tikaServerManagerSysProp) {
    this.tikaServerManagerSysProp = tikaServerManagerSysProp;
  }

  public String getTikaServerManagerSysProp() {
    return tikaServerManagerSysProp;
  }

  public long getIdleTikaProcessReaperMaxIdleMs() {
    return idleTikaProcessReaperMaxIdleMs;
  }

  public TikaServerManager setIdleTikaProcessReaperMaxIdleMs(long idleTikaProcessReaperMaxIdleMs) {
    this.idleTikaProcessReaperMaxIdleMs = idleTikaProcessReaperMaxIdleMs;
    return this;
  }

  public long getIdleTikaProcessReaperInitialDelay() {
    return idleTikaProcessReaperInitialDelay;
  }

  public TikaServerManager setIdleTikaProcessReaperInitialDelay(long idleTikaProcessReaperInitialDelay) {
    this.idleTikaProcessReaperInitialDelay = idleTikaProcessReaperInitialDelay;
    return this;
  }

  public long getIdleTikaProcessReaperDelay() {
    return idleTikaProcessReaperDelay;
  }

  public TikaServerManager setIdleTikaProcessReaperDelay(long idleTikaProcessReaperDelay) {
    this.idleTikaProcessReaperDelay = idleTikaProcessReaperDelay;
    return this;
  }

  public TimeUnit getIdleTikaProcessReaperDelayUnit() {
    return idleTikaProcessReaperDelayUnit;
  }

  public TikaServerManager setIdleTikaProcessReaperDelayUnit(TimeUnit idleTikaProcessReaperDelayUnit) {
    this.idleTikaProcessReaperDelayUnit = idleTikaProcessReaperDelayUnit;
    return this;
  }

  public boolean isAllowLocalFileOptimization() {
    return allowLocalFileOptimization;
  }

  public TikaServerManager setAllowLocalFileOptimization(boolean allowLocalFileOptimization) {
    this.allowLocalFileOptimization = allowLocalFileOptimization;
    return this;
  }

  public static void main(String[] args) throws Exception {
    TikaServerManager manager = args.length > 0 ? new TikaServerManager(args[0]) : new TikaServerManager();
    manager.init();
    manager.run(true);
  }
}
