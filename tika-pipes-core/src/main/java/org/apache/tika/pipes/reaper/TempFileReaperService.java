package org.apache.tika.pipes.reaper;

import java.io.File;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.tika.pipes.util.Props;

/**
 * A service that cleans up temp files from the work dir.
 */
public class TempFileReaperService {

  private static final Logger LOG = LoggerFactory.getLogger(TempFileReaperService.class);

  public static final TemporalUnit TEMP_FILE_REAPER_UNIT = ChronoUnit.MILLIS;
  public static final TimeUnit TEMP_FILE_REAPER_JOB_UNIT = TimeUnit.MILLISECONDS;
  public static final long TEMP_FILE_REAPER_MAX_FILE_AGE = Long.parseLong(Props.getProp("TEMP_FILE_REAPER_MAX_FILE_AGE", "6000000"));
  public static final long TEMP_FILE_REAPER_JOB_INITIAL_DELAY = Long.parseLong(Props.getProp("TEMP_FILE_REAPER_JOB_INITIAL_DELAY", "0"));
  public static final long TEMP_FILE_REAPER_JOB_DELAY = Long.parseLong(Props.getProp("TEMP_FILE_REAPER_JOB_DELAY", "60000"));
  public static final String TEMP_FILE_REAPER_ADDITIONAL_DIRECTORIES_TO_REAP = Props.getProp("TEMP_FILE_REAPER_ADDITIONAL_DIRECTORIES_TO_REAP", "");

  final private ScheduledExecutorService scheduledExecutorService;
  final private List<String> directoriesToReap;

  private static final String API_WORKDIR = System.getProperty("apollo.app.workDir.api");
  private static final String CONNECTORS_RPC_WORKDIR = System.getProperty("apollo.app.workDir.connectorsRpc");
  private static final String CONNECTORS_CLASSIC_WORKDIR = System.getProperty("apollo.app.workDir.connectorsClassic");

  /**
   * Create and run the service.
   *
   * @param directoriesToReap Directories where files are deleted from
   */
  public TempFileReaperService(List<String> directoriesToReap) {
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.directoriesToReap = Lists.newArrayList();
    this.directoriesToReap.addAll(directoriesToReap);
    for (String additionalFilePath : Arrays.asList(TEMP_FILE_REAPER_ADDITIONAL_DIRECTORIES_TO_REAP.split(","))) {
      if (StringUtils.isNotBlank(additionalFilePath)) {
        File additionalFile = new File(additionalFilePath);
        if (additionalFile.exists()) {
          this.directoriesToReap.add(additionalFile.getAbsolutePath());
        }
      }
    }
    start();
  }

  private void start() {
    scheduledExecutorService.scheduleWithFixedDelay(
        () -> {
          try {
            OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
            OffsetDateTime oldestTimeAllowed = now.minus(Duration.of(TEMP_FILE_REAPER_MAX_FILE_AGE, TEMP_FILE_REAPER_UNIT));

            Date threshold = Date.from(oldestTimeAllowed.toInstant());
            AgeFileFilter filter = new AgeFileFilter(threshold);

            int numDeleted = 0;
            for (String nextDirPathToDeleteFrom : directoriesToReap) {
              numDeleted += deleteOrphanedFiles(filter, new File(nextDirPathToDeleteFrom));
              LOG.debug("Tika fork parser's TempFileReaperService deleted {} temp files from {} during this iteration.",
                      numDeleted, nextDirPathToDeleteFrom);
            }
            if (API_WORKDIR != null && new File(API_WORKDIR).exists()) {
              numDeleted += deleteOrphanedFiles(filter, new File(API_WORKDIR));
            }
            if (CONNECTORS_CLASSIC_WORKDIR != null && new File(CONNECTORS_CLASSIC_WORKDIR).exists()) {
              numDeleted += deleteOrphanedFiles(filter, new File(CONNECTORS_CLASSIC_WORKDIR));
            }
            if (CONNECTORS_RPC_WORKDIR != null && new File(CONNECTORS_RPC_WORKDIR).exists()) {
              numDeleted += deleteOrphanedFiles(filter, new File(CONNECTORS_RPC_WORKDIR));
            }

          } catch (Exception e) {
            LOG.error("Could not run temp file reaper service", e);
            throw new RuntimeException("Could not run the schedule", e);
          }
        },
        TEMP_FILE_REAPER_JOB_INITIAL_DELAY,
        TEMP_FILE_REAPER_JOB_DELAY,
        TEMP_FILE_REAPER_JOB_UNIT);
    LOG.info("Started temp file reaper service: DirsToReap={}, ConnRpcWorkDir={}, ConnClassicWorkDir={}, ApiWorkDir={}, AdditionalDirs={}",
            directoriesToReap, CONNECTORS_RPC_WORKDIR, CONNECTORS_CLASSIC_WORKDIR, API_WORKDIR);
  }

  private int deleteOrphanedFiles(AgeFileFilter filter, File folder) {
    File[] filesInFolder = folder.listFiles();
    File[] expiredFiles = FileFilterUtils.filter(filter, filesInFolder);

    LOG.info("Checking folder \"{}\" for orphaned files. Total files in folder: {}, Number of these " +
        "files that are expired: {}", folder.getAbsolutePath(), filesInFolder.length, expiredFiles.length);

    int numDeleted = 0;

    for (File file : expiredFiles) {
      if (file.getAbsolutePath().contains("log4j")) {
        continue;
      }
      ++numDeleted;
      boolean deleted = FileUtils.deleteQuietly(file);
      LOG.info("Deleting an orphaned file \"{}\" - Succeeded={}", file.getAbsolutePath(), deleted);
    }
    return numDeleted;
  }

  /**
   * Shut down the executor, cancelling any pending jobs.
   */
  public void close() {
    if (!scheduledExecutorService.isShutdown()) {
      scheduledExecutorService.shutdownNow();
    }
  }

  public static void main(String [] args) {
    if (args.length == 0) {
      LOG.info("USAGE: TempFileReaperService [folderToReap1] [folderToReap2] ...");
      System.exit(1);
    }
    TempFileReaperService tempFileReaperService = new TempFileReaperService(Arrays.asList(args));
    tempFileReaperService.start();
  }
}
