package Logger;

import java.io.File;

public interface Logger {

	public void createLogFile(String logFilename);
	
	public void write(String logMessage, String threadName);
}
