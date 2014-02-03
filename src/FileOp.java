import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileOp {
	public static void writeToFile(String filePath, String content) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					filePath)));
			bw.write(content);
			bw.close();
		} catch (IOException ex) {
			System.err.println("Error: cannot open file: " + filePath);
			System.exit(1);
		}
	}
}
