package com.edu.uj.sk.btcg.logging;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.edu.uj.sk.btcg.collections.CCollections;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

public class CLogger {
	private Logger logger;
	
	private CLogger(Logger logger) {
		this.logger = logger;
		
		Optional<Handler> handler = 
			CCollections.find(this.logger.getHandlers(), new Predicate<Handler>() {
	
				@Override
				public boolean apply(Handler handler) {
					return handler instanceof ConsoleHandler;
				}
			});
		
		if (handler.isPresent()) {
			handler.get().setFormatter(new ConsoleFormatter());
		} else {
			ConsoleHandler consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(new ConsoleFormatter());
			
			this.logger.setUseParentHandlers(false);
			this.logger.addHandler(consoleHandler);
		}
	}
	
	public static CLogger getLogger(Class<?> clazz) {
		return new CLogger(Logger.getLogger(clazz.getName()));
	}
	
	
	
	
	public void warn(String formattedMsg, Object ...args) {
		logger.log(Level.WARNING, String.format(formattedMsg, args));
	}
	
	public void warn(String formattedMsg, Throwable exception, Object ...args) {
		logger.log(Level.WARNING, String.format(formattedMsg, args) + "\n" + getStackTrace(exception));
	}
	
	
	
	public void error(String formattedMsg, Object ...args) {
		logger.log(Level.SEVERE, String.format(formattedMsg, args));
	}
	
	public void error(String formattedMsg, Throwable exception, Object ...args) {
		logger.log(Level.SEVERE, String.format(formattedMsg, args), exception);
	}
	
	
	
	
	public void info(String formattedMsg, Object ...args) {
		logger.log(Level.INFO, String.format(formattedMsg, args));
	}
	
	
	
	
	private String getStackTrace(Throwable throwable) {
		return ExceptionUtils.getStackTrace(throwable);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private class ConsoleFormatter extends Formatter {
		
		@SuppressWarnings("deprecation")
		@Override
		public String format(LogRecord record) {
			return String.format("%s %s %s\n%s: %s\n", 
					new Date(record.getMillis()).toGMTString(),
					record.getMillis(),
					record.getLoggerName(),
					record.getLevel(),
					record.getMessage()
					);
		}
	}
}
