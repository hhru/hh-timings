package ru.hh.health.monitoring;

import java.util.Map;

public class TimingsLoggerFactory {

  // Event -> Delay
  private final Map<String, Long> probeDelays;

  public TimingsLoggerFactory(Map<String, Long> probeDelays) {
    this.probeDelays = probeDelays;
  }

  public TimingsLogger getLogger(String context, String requestId) {
    return new TimingsLogger(context, requestId, probeDelays);
  }
}
