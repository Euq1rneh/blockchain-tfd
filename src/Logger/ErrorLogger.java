package Logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ErrorLogger {

	public static final Logger logger = Logger.getLogger(ErrorLogger.class.getName());

	public static void setupLogger(String logFilename) throws IOException {
		LogManager.getLogManager().reset();

		FileHandler fileHandler = new FileHandler(logFilename+".log", true);
		fileHandler.setFormatter(new SimpleFormatter() {
			private static final String format = "[%1$tF %1$tT] [%2$s] %4$s: %5$s %n";

			@Override
			public synchronized String format(LogRecord record) {
				return String.format(format, record.getMillis(), Thread.currentThread().getName(),
						record.getLoggerName(), record.getLevel().getLocalizedName(), record.getMessage());
			}
		});

		// Set the handler and logging level
		logger.addHandler(fileHandler);
		logger.setLevel(Level.INFO);
	}
}
