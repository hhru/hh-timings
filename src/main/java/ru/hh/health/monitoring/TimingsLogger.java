package ru.hh.health.monitoring;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import ru.hh.util.LogLevel;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class TimingsLogger {
  private static final Logger LOG = LoggerFactory.getLogger(TimingsLogger.class);
  private static final String RECORDS_SPLITTER = "; ";

  private final Map<String, Long> probeDelays;
  private final String timingsContext;
  private final String requestId;
  private final List<LogRecord> logRecords = new ArrayList<>();

  private volatile int timedAreasCount;
  private volatile boolean errorState;
  private volatile long startTime;
  @Nullable
  private volatile String responseContext;

  public TimingsLogger(String context, String requestId, Map<String, Long> probeDelays) {
    this.timingsContext = context;
    this.requestId = requestId;
    this.probeDelays = probeDelays;
  }

  public synchronized void enterTimedArea() {
    if(startTime == 0)
      startTime = System.currentTimeMillis();
    timedAreasCount++;
  }

  public synchronized void leaveTimedArea() {
    if (timedAreasCount < 1) {
      throw new IllegalStateException();
    }
    timedAreasCount--;
    if(timedAreasCount == 0)
      outputLoggedTimings();
  }

  /**
   * Will cause all probes to be logged as ERROR when the timed area is left.
   */
  public synchronized void setErrorState() {
    errorState = true;
  }

  public synchronized void mark() {
    probe(null);
  }

  public synchronized void probe(String event) {
    Long delay = probeDelays.get(event);
    if(delay == null) {
      addLogRecord(event);
    } else {
      addLogRecord(format("Pausing before event %s for %d millis", event, delay));
      sleep(delay);
    }
  }

  public void setResponseContext(String responseContext) {
    this.responseContext = responseContext;
  }

  private static String probeMessage(long elapsed, String name) {
    return MessageFormatter.arrayFormat("{}=+{}", new Object[]{name, elapsed}).getMessage();
  }
  
  private void outputLoggedTimings() {
    final long endTime = System.currentTimeMillis();
    long timeSpent = endTime - startTime;
    StringBuilder logMessageBuilder = new StringBuilder();
    LoggingContext lc = LoggingContext.enter(requestId);
    try {
      logMessageBuilder.append("response: ").append(responseContext).append(RECORDS_SPLITTER);
      logMessageBuilder.append("context: ").append(timingsContext).append(RECORDS_SPLITTER);

      logMessageBuilder.append("total time ");
      logMessageBuilder.append(timeSpent);
      logMessageBuilder.append(" ms").append(RECORDS_SPLITTER);

      long prevTimestamp = startTime;
      for (LogRecord logRecord : logRecords) {
        if (!logRecord.isEmpty()) {
          logMessageBuilder.append(probeMessage(logRecord.timestamp - prevTimestamp, logRecord.message))
              .append(RECORDS_SPLITTER);
        }
        prevTimestamp = logRecord.timestamp;
      }
      LogLevel.log(LOG, errorState ? LogLevel.Level.ERROR : LogLevel.Level.INFO, logMessageBuilder.toString());
    } finally {
      lc.leave();
    }
  }

  private void addLogRecord(String message) {
    LogRecord logRecord = new LogRecord(message, System.currentTimeMillis());
    logRecords.add(logRecord);
  }

  private static void sleep(long delay) {
    try {
      Thread.sleep(delay);
    } catch(InterruptedException e) {
      LOG.warn("Interrupted", e);
      Thread.currentThread().interrupt();
    }
  }

  private static class LogRecord {
    public final Long timestamp;
    public final String message;

    LogRecord(String message, Long timestamp) {
      this.message = message;
      this.timestamp = timestamp;
    }
    
    public boolean isEmpty() {
      return message == null || message.isEmpty();      
    }
  }
}
