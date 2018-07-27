package ru.hh.health.monitoring;

import java.util.function.Supplier;
import org.slf4j.Logger;

public class LoggingStopWatchFactory {

  private final LoggingStopWatchWrapper wrapper;

  public LoggingStopWatchFactory(Logger logger, Supplier<? extends StopWatch> stopWatchSupplier, String context, String reqId) {
    wrapper = new LoggingStopWatchWrapper(logger, reqId, context, stopWatchSupplier.get());
  }

  public LoggingStopWatchWrapper getStopWatch(String stepName) {
    wrapper.incrementRCAndStart(stepName);
    return wrapper;
  }

  public interface StopWatch {
    void start(String stepName);
    void stop();
    boolean isStarted();
    @Override
    String toString();
  }
}
