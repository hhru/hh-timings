package ru.hh.health.monitoring;


public interface LogOutputPrototype {
  LogOutputPrototype MULTIPLE_STRING = new LogOutput.MultipleStringOutput();
  LogOutputPrototype ONE_STRING = new LogOutput.OneStringOutput();
  
  LogOutput get();
}
