package ru.hh.health.monitoring;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.InOrder;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TimingsLoggerTest {

  @Test
  public void testNoError() throws Exception {
    Logger logger = mock(Logger.class);
    TimingsLoggerFactory timingsLoggerFactory = new TimingsLoggerFactory(ImmutableMap.<String, Long>of(),
                                                                         Optional.of(10000l));
    TimingsLogger timingsLogger = timingsLoggerFactory.getLogger("context", "requestId");
    setFinalStaticField(TimingsLogger.class, "LOG", logger);

    timingsLogger.enterTimedArea();
    timingsLogger.probe("entry-point");
    timingsLogger.leaveTimedArea();

    verify(logger).debug(anyString());
  }

  @Test
  public void testExceeds() throws Exception {
    Logger logger = mock(Logger.class);
    TimingsLoggerFactory timingsLoggerFactory = new TimingsLoggerFactory(ImmutableMap.<String, Long>of(),
                                                                         Optional.of(500l));
    TimingsLogger timingsLogger = timingsLoggerFactory.getLogger("context", "requestId");
    setFinalStaticField(TimingsLogger.class, "LOG", logger);

    timingsLogger.enterTimedArea();
    sleep(550);
    timingsLogger.probe("entry-point");
    timingsLogger.leaveTimedArea();

    InOrder inOrder = inOrder(logger);
    inOrder.verify(logger).warn(contains("tolerance exceeded"));
    inOrder.verify(logger).warn(contains("entry-point"));
  }

  @Test
  public void testProbeSleep() throws Exception {
    Logger logger = mock(Logger.class);
    TimingsLoggerFactory timingsLoggerFactory = new TimingsLoggerFactory(ImmutableMap.<String, Long>of("entry-point", 110l),
                                                                         Optional.of(100l));
    TimingsLogger timingsLogger = timingsLoggerFactory.getLogger("context", "requestId");
    setFinalStaticField(TimingsLogger.class, "LOG", logger);

    timingsLogger.enterTimedArea();
    timingsLogger.probe("entry-point");
    timingsLogger.leaveTimedArea();

    InOrder inOrder = inOrder(logger);
    inOrder.verify(logger).warn(contains("tolerance exceeded"));
    inOrder.verify(logger).warn(contains("entry-point"));
  }

  @Test
  public void testError() throws Exception {
    Logger logger = mock(Logger.class);
    TimingsLoggerFactory timingsLoggerFactory = new TimingsLoggerFactory(ImmutableMap.<String, Long>of(),
                                                                         Optional.of(10000l));
    TimingsLogger timingsLogger = timingsLoggerFactory.getLogger("context", "requestId");
    setFinalStaticField(TimingsLogger.class, "LOG", logger);

    timingsLogger.enterTimedArea();
    timingsLogger.probe("entry-point");
    timingsLogger.setErrorState();
    timingsLogger.leaveTimedArea();

    InOrder inOrder = inOrder(logger);
    inOrder.verify(logger).error(contains("total"));
    inOrder.verify(logger).error(contains("entry-point"));
  }

  private static <T> void setFinalStaticField(Class<T> clazz, String fieldName, Object newValue) throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);

    // remove final modifier from field
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(null, newValue);
  }

  private static void sleep(long delay) {
    try {
      Thread.sleep(delay);
    } catch(InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
