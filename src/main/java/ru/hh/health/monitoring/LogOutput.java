package ru.hh.health.monitoring;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import ru.hh.util.LogLevel;


public abstract class LogOutput {
  protected Logger logger;
  protected LogLevel.Level level;  
  protected String context;

  protected LogOutput() {}

  public abstract void collect(String message);

  public void dump() {}
  
  private void setContext(@Nullable String context) {
    this.context = context;
  }

  private void setLogger(Logger logger) {
    this.logger = logger;
  }

  private void setLevel(LogLevel.Level level) {
    this.level = level;
  }


  public static LogOutput[] make(Logger logger, LogLevel.Level level, String context, LogOutputPrototype... prototypes) {
    assert logger != null;
    assert level != null;
    assert prototypes != null;

    LogOutput[] outputs = new LogOutput[prototypes.length];
    for (int i = 0; i < prototypes.length; ++i) {
      LogOutput out = outputs[i] = prototypes[i].get();
      out.setLogger(logger);
      out.setLevel(level);
      out.setContext(context);
    }
    return outputs;
  }

  public static void collect(String message, LogOutput... outs) {
    for (LogOutput out : outs) {
      out.collect(message);
    }
  }

  public static void dump(LogOutput... outs) {
    for (LogOutput out : outs) {
      out.dump();
    }
  }


  public static class OneStringOutput extends LogOutput implements LogOutputPrototype {
    private final StringBuilder result = new StringBuilder();
    
    @Override
    public void collect(String message) {
      result.append(message).append("; ");
    }

    @Override
    public void dump() {
      LogLevel.log(logger, level, result.toString());
    }
    
    @Override
    public LogOutput get() {
      return new OneStringOutput();
    }
  }
  
  
  public static class MultipleStringOutput extends LogOutput implements LogOutputPrototype {
    @Override
    public void collect(String message) {
      LogLevel.log(logger, level, "Context : " + context + " ; " + message);      
    }

    @Override
    public LogOutput get() {
      return new MultipleStringOutput();
    }
  }  
}

