package ru.hh.health.monitoring;

import com.google.common.base.Optional;

import java.util.Map;

public class TimingsLoggerFactory {

  public static final long DEFAULT_TIME_TOLERANCE = 15000; // 15 seconds

  // Event -> Delay
  private final Map<String, Long> probeDelays;
  private final long totalTimeThreshold;
  private final LogOutputPrototype[] prototypes;

  public TimingsLoggerFactory(Map<String, Long> probeDelays, Optional<Long> totalTimeThreshold, LogOutputPrototype... prototypes) {
    this.prototypes = prototypes;
    this.probeDelays = probeDelays;
    this.totalTimeThreshold = (totalTimeThreshold.isPresent())
        ? totalTimeThreshold.get()
        : DEFAULT_TIME_TOLERANCE;
  }

  public TimingsLoggerFactory(Map<String, Long> probeDelays, Optional<Long> totalTimeThreshold) {
    this(probeDelays, totalTimeThreshold, LogOutputPrototype.MULTIPLE_STRING);
  }

  public TimingsLogger getLogger(String context, String requestId) {
    return new TimingsLogger(context, requestId, probeDelays, totalTimeThreshold, prototypes);
  }

  public long getTotalTimeThreshold() {
    return totalTimeThreshold;
  }
}
