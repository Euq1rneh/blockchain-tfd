package streamlet;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class AAAAAAA {

	public static void main(String[] args) {
		
		Random rd = new Random(123456789);
		
		
        try (FileWriter writer = new FileWriter("rnd-values.txt")) {
        	
        	for (int i = 0; i < 100; i++) {
        		writer.write((rd.nextInt(100)%5) + 1 +"\n");	
			}            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
