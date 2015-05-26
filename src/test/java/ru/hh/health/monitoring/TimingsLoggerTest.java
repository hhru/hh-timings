package ru.hh.health.monitoring;

import com.google.common.collect.ImmutableMap;
import junit.framework.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TimingsLoggerTest {

  @Test
  public void testNoError() throws Exception {
    Logger logger = mock(Logger.class);
    TimingsLoggerFactory timingsLoggerFactory = new TimingsLoggerFactory(ImmutableMap.<String, Long>of());
    TimingsLogger timingsLogger = timingsLoggerFactory.getLogger("context", "requestId");
    setFinalStaticField(TimingsLogger.class, "LOG", logger);

    timingsLogger.enterTimedArea();
    timingsLogger.probe("entry-point");
    timingsLogger.leaveTimedArea();

    verify(logger).info(anyString());
  }

  @Test
  public void testError() throws Exception {
    Logger logger = mock(Logger.class);
    TimingsLoggerFactory timingsLoggerFactory = new TimingsLoggerFactory(ImmutableMap.<String, Long>of());
    TimingsLogger timingsLogger = timingsLoggerFactory.getLogger("context", "requestId");
    setFinalStaticField(TimingsLogger.class, "LOG", logger);

    timingsLogger.enterTimedArea();
    timingsLogger.probe("entry-point");
    timingsLogger.setErrorState();
    timingsLogger.leaveTimedArea();

    InOrder inOrder = inOrder(logger);
    ArgumentCaptor<String> errorMessage = ArgumentCaptor.forClass(String.class);
    inOrder.verify(logger).error(errorMessage.capture());
    Assert.assertTrue(errorMessage.getValue().contains("total"));
    Assert.assertTrue(errorMessage.getValue().contains("entry-point"));
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
}
