package Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ErrorLogger implements Logger {

	File logFile = null;
	
	@Override
	public void createLogFile(String logFilename) {
		File f = new File(logFilename);
		
		if(f.exists()) {
			f.delete();
		}
		
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
		
		sb.append("----------"+ threadName +"----------");
		sb.append(logMessage);
		
		try (FileWriter writer = new FileWriter(logFile)) {			
            writer.write(sb.toString());
        } catch (IOException e) {
            System.out.println("An error occurred while writing the error log file.");
            e.printStackTrace();
        }
	}

}
