package ru.hh.health.monitoring;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

public class LoggingStopWatchWrapper implements AutoCloseable {

  private final Logger logger;
  private final String requestId;
  private final String context;

  private final LoggingStopWatchFactory.StopWatch stopWatch;
  private final AtomicInteger referenceCounter;

  protected LoggingStopWatchWrapper(Logger logger, String requestId, String context, LoggingStopWatchFactory.StopWatch stopWatch) {
    this.logger = logger;
    this.requestId = requestId;
    this.context = context;
    this.stopWatch = stopWatch;
    referenceCounter = new AtomicInteger(0);
  }

  void incrementRCAndStart(String stepName) {
    referenceCounter.incrementAndGet();
    probe(stepName);
  }

  public void probe(String stepName) {
    stopIfStarted();
    stopWatch.start(stepName);
  }

  @Override
  public void close() {
    stopIfStarted();
    int counter = referenceCounter.decrementAndGet();
    if (counter < 0) {
      logger.error("Something wrong with StopWatch!!! Context: {}, counter={}", context, counter);
    }
    if (counter <= 0) {
      try (LoggingContext lc = LoggingContext.enter(requestId)) {
        logger.debug("{}: {}", context, stopWatch);
      }
    }
  }

  private void stopIfStarted() {
    if (stopWatch.isStarted()) {
      stopWatch.stop();
    }
  }
}
