package Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ProcessLogger implements Logger {

	File logFile = null;

	@Override
	public void createLogFile(String logFilename) {
		File f = new File(logFilename);

		if (f.exists())
			return;

		try {
			f.createNewFile();
			logFile = f;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void write(String logMessage, String threadName) {
		if(logFile == null)return;
		
		StringBuilder sb = new StringBuilder();

		sb.append("----------" + threadName + "----------\n");
		sb.append(logMessage);

		try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) { // Append mode
            writer.println(sb.toString()); // Print with automatic line break
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the process log file.");
        }
	}

}
