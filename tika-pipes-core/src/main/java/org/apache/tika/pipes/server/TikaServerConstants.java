package org.apache.tika.pipes.server;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.tika.pipes.util.Props;

public final class TikaServerConstants {
  private static final Logger log = LoggerFactory.getLogger(TikaServerConstants.class);

  public static final int TIKA_SERVER_CLIENT_CONNECT_TIMEOUT = Integer.parseInt(Props.getProp("TIKA_SERVER_CLIENT_CONNECT_TIMEOUT", "60000"));

  public static final int TIKA_SERVER_MANAGER_PORT = Integer.parseInt(Props.getProp("TIKA_SERVER_MANAGER_PORT", "8889"));
  public static final int SPAWNED_SERVER_PORT_RANGE_START = Integer.parseInt(Props.getProp("SPAWNED_SERVER_PORT_RANGE_START", "49300"));
  public static final int SPAWNED_SERVER_PORT_RANGE_END = Integer.parseInt(Props.getProp("SPAWNED_SERVER_PORT_RANGE_END", "49399"));

  public static final boolean TIKA_SERVER_INHERIT_SERVER_LOGS = Boolean.parseBoolean(Props.getProp("TIKA_SERVER_INHERIT_SERVER_LOGS", "true"));
  public static final boolean ALLOW_LOCAL_FILE_OPTIMIZATION = Boolean.parseBoolean(Props.getProp("ALLOW_LOCAL_FILE_OPTIMIZATION", "true"));

  public static final int MAX_HEAP_MB = Integer.parseInt(Props.getProp("MAX_HEAP_MB", "300"));
  public static final int NUM_PROCESSES = Integer.parseInt(Props.getProp("NUM_PROCESSES", "5"));
  public static final int PROCESS_READY_RETRIES = Integer.parseInt(Props.getProp("PROCESS_READY_RETRIES", "60"));
  public static final int PROCESS_READY_DELAY_MS = Integer.parseInt(Props.getProp("PROCESS_READY_DELAY_MS", "1000"));
  public static final int PROCESS_DEAD_RETRIES = Integer.parseInt(Props.getProp("PROCESS_DEAD_RETRIES", "60"));
  public static final int PROCESS_DEAD_DELAY_MS = Integer.parseInt(Props.getProp("PROCESS_DEAD_DELAY_MS", "1000"));
  public static final int MAX_FILES = Integer.parseInt(Props.getProp("MAX_FILES", "2000"));

  /**
   * If the child doesn't receive a ping or the parent doesn't
   * hear back from a ping in this amount of time, kill and restart the child.
   */
  public static final long PING_TIMEOUT_MILLIS = Long.parseLong(Props.getProp("PING_TIMEOUT_MILLIS", "30000"));

  /**
   * How often should the parent try to ping the child to check status
   */
  public static final long PING_PULSE_MILLIS = Long.parseLong(Props.getProp("PING_PULSE_MILLIS", "500"));

  /**
   * Number of milliseconds to wait per server task (parse, detect, unpack, translate,
   * etc.) before timing out and shutting down the child process.
   */
  public static final long TASK_TIMEOUT_MILLIS = Long.parseLong(Props.getProp("TASK_TIMEOUT_MILLIS", "120000"));

  /**
   * Number of milliseconds to wait per server task (parse, detect, unpack, translate,
   * etc.) before timing out and shutting down the child process.
   */
  public static final long TASK_PULSE_MILLIS = Long.parseLong(Props.getProp("TASK_PULSE_MILLIS", "500"));

  /**
   * Number of milliseconds to wait for child process to startup
   */
  public static final long CHILD_STARTUP_MILLIS = Long.parseLong(Props.getProp("CHILD_STARTUP_MILLIS", "120000"));

  public static final long IDLE_TIKA_PROCESS_REAPER_MAX_IDLE_MS = Long.parseLong(Props.getProp("IDLE_TIKA_PROCESS_REAPER_MAX_IDLE_MS", "150000"));
  public static final TimeUnit IDLE_TIKA_PROCESS_REAPER_DELAY_UNIT = TimeUnit.MILLISECONDS;
  public static final long IDLE_TIKA_PROCESS_REAPER_INITIAL_DELAY = Long.parseLong(Props.getProp("IDLE_TIKA_PROCESS_REAPER_INITIAL_DELAY", "60000"));
  public static final long IDLE_TIKA_PROCESS_REAPER_DELAY = Long.parseLong(Props.getProp("IDLE_TIKA_PROCESS_REAPER_DELAY", "30000"));

  public static final int HTTPCLIENT_REQUEST_TIMEOUT = Integer.parseInt(Props.getProp("HTTPCLIENT_REQUEST_TIMEOUT", "300000"));
  public static final int HTTPCLIENT_CONNECT_TIMEOUT = Integer.parseInt(Props.getProp("HTTPCLIENT_CONNECT_TIMEOUT", "5000"));
  public static final int HTTPCLIENT_SOCKET_TIMEOUT = Integer.parseInt(Props.getProp("HTTPCLIENT_SOCKET_TIMEOUT", "30000"));
  public static final int HTTPCLIENT_DEFAULT_MAX_PER_ROUTE = Integer.parseInt(Props.getProp("HTTPCLIENT_DEFAULT_MAX_PER_ROUTE", "1000"));
  public static final int HTTPCLIENT_DEFAULT_MAX_TOTAL = Integer.parseInt(Props.getProp("HTTPCLIENT_DEFAULT_MAX_TOTAL", "5000"));
  public static final String DTIKA_CMD_UNIQUE_SYSPROP = "-DtikaUniqueId";
  public static final String TIKA_LOG4J_XML_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">\n" +
      "<log4j:configuration>\n" +
      "    <appender name=\"stdout\" class=\"org.apache.log4j.ConsoleAppender\">\n" +
      "        <param name=\"Target\" value=\"System.out\"/>\n" +
      "        <layout class=\"org.apache.log4j.PatternLayout\">\n" +
      "            <!-- Pattern to output the caller's file name and line number -->\n" +
      "            <!--<param name=\"ConversionPattern\" value=\"%5p [%t] (%F:%L) - %m%n\"/>-->\n" +
      "            <param name=\"ConversionPattern\" value=\"%m%n\"/>\n" +
      "        </layout>\n" +
      "    </appender>\n" +
      "    <appender name=\"stderr\" class=\"org.apache.log4j.ConsoleAppender\">\n" +
      "        <param name=\"Target\" value=\"System.err\"/>\n" +
      "        <layout class=\"org.apache.log4j.PatternLayout\">\n" +
      "            <!-- Pattern to output the caller's file name and line number -->\n" +
      "            <!--<param name=\"ConversionPattern\" value=\"%5p [%t] (%F:%L) - %m%n\"/>-->\n" +
      "            <param name=\"ConversionPattern\" value=\"%m%n\"/>\n" +
      "        </layout>\n" +
      "    </appender>\n" +
      "    <root>\n" +
      "        <level value=\"" + Level.toLevel(Props.getProp("TIKA_LOG_LEVEL", "ERROR")).toString() + "\" />\n" +
      "        <appender-ref ref=\"stdout\" />\n" +
      "    </root>\n" +
      "</log4j:configuration>";

  private TikaServerConstants() {
    // final
  }
}
