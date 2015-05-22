package ru.hh.health.monitoring;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import ru.hh.util.LogLevel;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class TimingsLogger {
  private final static Logger LOG = LoggerFactory.getLogger(TimingsLogger.class);
  public static final String RECORDS_SPLITTER = " ; ";

  private final Map<String, Long> probeDelays;
  private final String timingsContext;
  private final String requestId;
  private final List<LogRecord> logRecords = newArrayList();

  private volatile int timedAreasCount;
  private volatile boolean errorState;
  private volatile long startTime;
  

  public TimingsLogger(String context, String requestId, Map<String, Long> probeDelays) {
    this.timingsContext = context;
    this.requestId = requestId;
    this.probeDelays = probeDelays;
  }

  public synchronized void enterTimedArea() {
    if(startTime == 0)
      startTime = DateTimeUtils.currentTimeMillis();
    timedAreasCount++;
  }

  public synchronized void leaveTimedArea() {
    checkState(timedAreasCount >= 1);
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

  private String probeMessage(long elapsed, String name) {
    return MessageFormatter.arrayFormat("'{}'=+{}", new Object[]{name, elapsed}).getMessage();
  }
  
  private void outputLoggedTimings() {
    final long endTime = DateTimeUtils.currentTimeMillis();
    long timeSpent = endTime - startTime;
    StringBuilder logMessageBuilder = new StringBuilder();
    LoggingContext lc = LoggingContext.enter(requestId);
    try {
      if(StringUtils.isNotBlank(timingsContext)) {
        logMessageBuilder.append("Context : ").append(timingsContext).append(RECORDS_SPLITTER);
      }
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
    LogRecord logRecord = new LogRecord(message, DateTimeUtils.currentTimeMillis());
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

    private LogRecord(String message, Long timestamp) {
      this.message = message;
      this.timestamp = timestamp;
    }
    
    public boolean isEmpty() {
      return message == null || message.isEmpty();      
    }
  }
}
