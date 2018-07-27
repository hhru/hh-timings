package ru.hh.health.monitoring;

import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class LoggingStopWatchWrapperTest {

  private LoggingStopWatchFactory factory;
  private LoggingStopWatchFactory.StopWatch stopWatch;

  @Before
  public void setUp() {
    stopWatch = crateTestStopWatch();
    factory = new LoggingStopWatchFactory(mock(Logger.class), () -> stopWatch, "testCtx", "noRequestId");
  }

  private LoggingStopWatchFactory.StopWatch crateTestStopWatch() {
    LoggingStopWatchFactory.StopWatch stopWatch = mock(LoggingStopWatchFactory.StopWatch.class);
    AtomicBoolean stateHolder = new AtomicBoolean(false);
    doAnswer(inv -> {
      stateHolder.set(true);
      return null;
    }).when(stopWatch).start(anyString());
    doAnswer(inv -> {
      stateHolder.set(false);
      return null;
    }).when(stopWatch).stop();
    when(stopWatch.isStarted()).thenAnswer(inv -> stateHolder.get());
    return stopWatch;
  }

  @Test
  public void testNestedBlocksWithProbeBeforeClose() {
    try (LoggingStopWatchWrapper stopWatch = factory.getStopWatch("outer")) {
      stopWatch.probe("first outer probe");
      System.out.println("Executing outer before inner");
      innerMethod();
      stopWatch.probe("second outer probe");
      System.out.println("Executing outer after inner");
    }
    verify(stopWatch, times(6)).start(anyString());
    verify(stopWatch, times(6)).stop();
  }

  @Test
  public void testNestedBlocksWithInnerWithoutProbe() {
    try (LoggingStopWatchWrapper stopWatch = factory.getStopWatch("outer")) {
      stopWatch.probe("first outer probe");
      System.out.println("Executing outer before inner");
      innerMethodWithoutProbe();
      stopWatch.probe("second outer probe");
      System.out.println("Executing outer after inner");
    }
    verify(stopWatch, times(4)).start(anyString());
    verify(stopWatch, times(4)).stop();
  }

  @Test
  public void testNestedBlocksWithOuterWithoutProbe() {
    try (LoggingStopWatchWrapper stopWatch = factory.getStopWatch("outer")) {
      System.out.println("Executing outer before inner");
      innerMethod();
      System.out.println("Executing outer after inner");
    }
    verify(stopWatch, times(4)).start(anyString());
    verify(stopWatch, times(4)).stop();
  }

  @Test
  public void testNestedBlocksWithoutProbe() {
    try (LoggingStopWatchWrapper stopWatch = factory.getStopWatch("outer")) {
      System.out.println("Executing outer before inner");
      innerMethodWithoutProbe();
      System.out.println("Executing outer after inner");
    }
    verify(stopWatch, times(2)).start(anyString());
    verify(stopWatch, times(2)).stop();
  }

  private void innerMethodWithoutProbe() {
    try (LoggingStopWatchWrapper stopWatch = factory.getStopWatch("inner")) {
      System.out.println("Executing inner");
    }
  }

  private void innerMethod() {
    try (LoggingStopWatchWrapper stopWatch = factory.getStopWatch("inner")) {
      stopWatch.probe("first inner probe");
      System.out.println("Executing inner");
      stopWatch.probe("second inner probe");
    }
  }

  @Test
  public void testNestedBlockConcurrently() throws InterruptedException {
    Exchanger<Void> exchanger = new Exchanger<>();
    try (LoggingStopWatchWrapper stopWatch = factory.getStopWatch("outer")) {
      stopWatch.probe("first outer probe");
      System.out.println("Executing outer before inner");
      new Thread(() -> {
        innerMethod();
        try {
          exchanger.exchange(null);
        } catch (InterruptedException ignored) {
        }
      }).start();
      stopWatch.probe("second outer probe");
      System.out.println("Executing outer after inner");
    }
    exchanger.exchange(null);
    verify(stopWatch, times(6)).start(anyString());
    verify(stopWatch, times(6)).stop();
  }
}
