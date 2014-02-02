
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

enum RetrievalAlgorithm {
	UNRANKEDBOOLEAN, RANKEDBOOLEAN
}

public class QryParams {

	public static String queryFilePath;
	public static String indexPath;
	public static String trecEvalOutPath;
	public static RetrievalAlgorithm retrievalAlgm;

	public static void parseParameterFile(String filePath) {
		// read in the parameter file; one parameter per line in format of
		// key=value
		Map<String, String> params = new HashMap<String, String>();
		try {
			Scanner scan = new Scanner(new File(filePath));
			String line = null;
			do {
				line = scan.nextLine();
				String[] pair = line.split("=");
				params.put(pair[0].trim(), pair[1].trim());
			} while (scan.hasNext());

			scan.close();
		} catch (FileNotFoundException ex) {
			System.err.println("Error: cannot file: " + filePath);
			System.exit(1);
		}

		// parameters required for this example to run
		if (!params.containsKey("queryFilePath")
				|| !params.containsKey("indexPath")
				|| !params.containsKey("trecEvalOutputPath")
				|| !params.containsKey("retrievalAlgorithm")) {
			System.err.println("Error: Parameters were missing.");
			System.exit(1);
		}

		// store the parameters in static variables
		queryFilePath = params.get("queryFilePath");
		indexPath = params.get("indexPath");
		trecEvalOutPath = params.get("trecEvalOutputPath");
		if (params.get("retrievalAlgorithm").equals("UnrankedBoolean"))
			retrievalAlgm = RetrievalAlgorithm.UNRANKEDBOOLEAN;
		else if (params.get("retrievalAlgorithm").equals("RankedBoolean"))
			retrievalAlgm = RetrievalAlgorithm.RANKEDBOOLEAN;
		else {
			System.err.println("Error: unidentified retrieval algorithm.");
			System.exit(1);
		}
	}
}
