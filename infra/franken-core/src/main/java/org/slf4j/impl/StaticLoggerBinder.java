package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder
  implements LoggerFactoryBinder
{
  private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

  public static String REQUESTED_API_VERSION = "1.6.99";

  private static final String loggerFactoryClassStr = KrakenLoggerFactory.class.getName();
  private final ILoggerFactory loggerFactory;

  public static final StaticLoggerBinder getSingleton()
  {
    return SINGLETON;
  }

  private StaticLoggerBinder()
  {
    this.loggerFactory = new KrakenLoggerFactory();
  }

  public ILoggerFactory getLoggerFactory() {
    return this.loggerFactory;
  }

  public String getLoggerFactoryClassStr() {
    return loggerFactoryClassStr;
  }
}