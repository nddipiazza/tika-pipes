package org.apache.tika.pipes.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.apache.tika.pipes.util.OSUtils;

public class TikaServer implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(TikaServer.class);
  private static final Gson GSON = new Gson();
  private String host;
  private int port;
  private int maxHeapMb;
  private boolean started;
  private Process process;
  private String workDirectoryPath;
  private String tikaServerJarPath;
  private String tikaEndpoint;
  private String tikaServerManagerSysProp;
  final private boolean logTikaServerOutput;
  final private long pingTimeoutMillis;
  final private long pingPulseMillis;
  final private long taskTimeoutMillis;
  final private long taskPulseMillis;
  final private long childStartupMillis;
  final private boolean allowLocalFileOptimization;

  public TikaServer(String tikaServerJarPath,
                    String workDirectoryPath,
                    int maxHeapMb,
                    boolean logTikaServerOutput,
                    long pingTimeoutMillis,
                    long pingPulseMillis,
                    long taskTimeoutMillis,
                    long taskPulseMillis,
                    long childStartupMillis,
                    boolean allowLocalFileOptimization,
                    String tikaServerManagerSysProp) {
    this.tikaServerJarPath = tikaServerJarPath;
    this.workDirectoryPath = workDirectoryPath;
    this.maxHeapMb = maxHeapMb;
    this.logTikaServerOutput = logTikaServerOutput;
    this.pingTimeoutMillis = pingTimeoutMillis;
    this.pingPulseMillis = pingPulseMillis;
    this.taskTimeoutMillis = taskTimeoutMillis;
    this.taskPulseMillis = taskPulseMillis;
    this.childStartupMillis = childStartupMillis;
    this.allowLocalFileOptimization = allowLocalFileOptimization;
    this.tikaServerManagerSysProp = tikaServerManagerSysProp;
  }

  public String getTikaEndpoint() {
    return tikaEndpoint;
  }

  /**
   * Starts the tika server. This method is synchronized to force tika servers to start one-at-a-time.
   */
  public synchronized void startServer() throws IOException {
    if (isRunning()) {
      return;
    }
    host = System.getProperty("tika.remote.bindAddress", System.getProperty("com.lucidworks.apollo.app.bindAddress"));
    port = OSUtils.findAvailablePort(
        TikaServerConstants.SPAWNED_SERVER_PORT_RANGE_START, TikaServerConstants.SPAWNED_SERVER_PORT_RANGE_END);

    File workDir = new File(workDirectoryPath);
    File log4jTmpDir = new File(workDir, "tika-lw-log4j");
    log4jTmpDir.mkdirs();
    File log4jFile = new File(log4jTmpDir, "log4j_server.xml");

    FileUtils.writeStringToFile(log4jFile, TikaServerConstants.TIKA_LOG4J_XML_TEMPLATE, StandardCharsets.UTF_8);

    String tikaServerUniqueId = TikaServerConstants.DTIKA_CMD_UNIQUE_SYSPROP + "=" + UUID.randomUUID();
    List<String> tikaCommand = new ArrayList<>(Arrays.asList("java",
        "-XX:+SuppressFatalErrorMessage",
        TikaServerConstants.TIKA_CMD_LINE_IDENTIFIER,
        tikaServerManagerSysProp,
        tikaServerUniqueId,
        "-Dlog4j.configuration=" + log4jFile.toURI(),
        "-jar", tikaServerJarPath,
        "-s",
        "-spawnChild",
        "-status",
        "-maxChildStartupMillis",
        String.valueOf(childStartupMillis),
        "-pingPulseMillis",
        String.valueOf(pingPulseMillis),
        "-pingTimeoutMillis",
        String.valueOf(pingTimeoutMillis),
        "-taskPulseMillis",
        String.valueOf(taskPulseMillis),
        "-taskTimeoutMillis",
        String.valueOf(taskTimeoutMillis),
        "-JXmx" + maxHeapMb + "m",
        "-JXX:+SuppressFatalErrorMessage",
        "-JDjava.io.tmpdir=" + workDirectoryPath,
        "-port", String.valueOf(port),
        "-host", StringUtils.isBlank(host) ? "0.0.0.0" : host,
        "-JDlog4j.configuration=" + log4jFile.toURI()
    ));

    if (allowLocalFileOptimization) {
      tikaCommand.add("-enableUnsecureFeatures");
      tikaCommand.add("-enableFileUrl");
    }
    if (TikaServerConstants.MAX_FILES > 0) {
      tikaCommand.add("-maxFiles");
      tikaCommand.add(String.valueOf(TikaServerConstants.MAX_FILES));
    }

    log.info("Starting tika remote server with cmd line: {}", StringUtils.join(tikaCommand, " "));

    process = new ProcessBuilder()
        .command(tikaCommand)
        .start();

    String tikaServerHost = host;
    if (StringUtils.isBlank(host) || "0.0.0.0".equals(host)) {
      tikaServerHost = InetAddress.getLocalHost().getHostAddress();
    }
    tikaEndpoint = String.format("http://%s:%d", tikaServerHost, port);

    inheritIO(process.getInputStream());
    inheritIO(process.getErrorStream());

    if (!process.isAlive()) {
      throw new TikaServerException("Could not start " + StringUtils.join(tikaCommand, " "));
    }
    started = true;

    waitForServerToStart();
  }

  private void waitForServerToStart() {
    int retries = TikaServerConstants.PROCESS_READY_RETRIES;
    while (--retries > 0 && !isRunning()) {
      log.info("Still waiting for Tika server on port {} to start. Retries remaining: {}", port, retries);
      try {
        Thread.sleep(TikaServerConstants.PROCESS_READY_DELAY_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    if (retries <= 0) {
      throw new TikaServerException("Tika remote server failed to start after 10 attempts");
    }
  }

  private void inheritIO(final InputStream src) {
    if (logTikaServerOutput) {
      new Thread(() -> {
        MDC.put("url", tikaEndpoint);
        Scanner sc = new Scanner(src);
        while (sc.hasNextLine()) {
          String nextLine = sc.nextLine();
          if (nextLine != null) {
            log.info("tika-remote: {}", nextLine);
          }
        }
      }).start();
    } else {
      new Thread(() -> {
        MDC.put("url", tikaEndpoint);
        Scanner sc = new Scanner(src);
        while (sc.hasNextLine()) {
          sc.nextLine();
        }
      }).start();
    }
  }

  static Map getTikaServerStatus(String host, int port) throws IOException {
    try (CloseableHttpClient client = HttpClients.createMinimal()) {
      host = StringUtils.isBlank(host) || "0.0.0.0".equals(host) ? InetAddress.getLocalHost().getHostAddress() : host;
      try (CloseableHttpResponse response = client.execute(new HttpGet("http://" + host + ":" + port + "/status"))) {
        String payload = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new IOException("Could not get status from tika server on port " + port + ", error payload: " + payload);
        }
        return GSON.fromJson(payload, Map.class);
      }
    }
  }

  private String getClientHost() throws IOException {
    if (StringUtils.isBlank(host) || "0.0.0.0".equals(host)) {
      return InetAddress.getLocalHost().getHostAddress();
    }
    return host;
  }

  public boolean isRunning() {
    if (!started) {
      return false;
    }
    try {
      Map res = getTikaServerStatus(getClientHost(), port);
      return "OPERATING".equalsIgnoreCase((String)res.get("status"));
    } catch (IOException ioe) {
      return false;
    }
  }

  @Override
  public void close() {
    if (process != null && process.isAlive()) {
      try {
        process.destroyForcibly();
        int maxWaitFor = TikaServerConstants.PROCESS_DEAD_RETRIES;
        while (process.isAlive() && --maxWaitFor > 0) {
          try {
            Thread.sleep(TikaServerConstants.PROCESS_DEAD_DELAY_MS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
        if (maxWaitFor <= 0) {
          throw new TikaServerException("Could not kill tika server process. Waited " +
              TikaServerConstants.PROCESS_DEAD_RETRIES * TikaServerConstants.PROCESS_DEAD_DELAY_MS + " ms but process is still running.");
        }
        started = false;
        log.info("Stopped tika fork server that was running on port {}", port);
        port = 0;
      } catch (Exception e) {
        log.debug("Failed to stop the tika remote process", e);
      }
    }
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("host", host)
        .add("port", port)
        .add("maxHeapMb", maxHeapMb)
        .add("started", started)
        .add("process", process)
        .add("workDirectoryPath", workDirectoryPath)
        .add("tikaServerJarPath", tikaServerJarPath)
        .add("tikaEndpoint", tikaEndpoint)
        .add("logTikaServerOutput", logTikaServerOutput)
        .add("pingTimeoutMillis", pingTimeoutMillis)
        .add("pingPulseMillis", pingPulseMillis)
        .add("taskTimeoutMillis", taskTimeoutMillis)
        .add("taskPulseMillis", taskPulseMillis)
        .add("childStartupMillis", childStartupMillis)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TikaServer that = (TikaServer) o;
    return port == that.port &&
        Objects.equals(host, that.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port);
  }
}
