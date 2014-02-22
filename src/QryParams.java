import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

enum RetrievalAlgorithm {
	UNRANKEDBOOLEAN, RANKEDBOOLEAN, BM25, INDRI
}

enum SmoothTech {
	NONE, DF, CTF
}

public class QryParams {

	public static String queryFilePath;
	public static String indexPath;
	public static String trecEvalOutPath;
	public static RetrievalAlgorithm retrievalAlgm;

	// BM25 parameters
	public static double BM25_k1 = -1;
	public static double BM25_k3 = -1;
	public static double BM25_b = -1;

	// Indri parameters
	public static int Indri_mu = -1;
	public static double Indri_lambda = -1;
	public static SmoothTech Indri_smo = SmoothTech.NONE;

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
		if (params.get("retrievalAlgorithm")
				.equalsIgnoreCase("UnrankedBoolean"))
			retrievalAlgm = RetrievalAlgorithm.UNRANKEDBOOLEAN;
		else if (params.get("retrievalAlgorithm").equalsIgnoreCase(
				"RankedBoolean"))
			retrievalAlgm = RetrievalAlgorithm.RANKEDBOOLEAN;
		else if (params.get("retrievalAlgorithm").equalsIgnoreCase("BM25")) {
			retrievalAlgm = RetrievalAlgorithm.BM25;
			BM25_k1 = Double.parseDouble(params.get("BM25:k_1"));
			BM25_k3 = Double.parseDouble(params.get("BM25:k_3"));
			BM25_b = Double.parseDouble(params.get("BM25:b"));

			// validation check
			if (BM25_k1 < 0.0 || BM25_k3 < 0.0
					|| (BM25_b < 0.0 || BM25_b > 1.0)) {
				System.err.println("Error: invalid parameters in: BM25");
				System.exit(1);
			}
		} else if (params.get("retrievalAlgorithm").equalsIgnoreCase("Indri")) {
			retrievalAlgm = RetrievalAlgorithm.INDRI;
			Indri_mu = Integer.parseInt(params.get("Indri:mu"));
			Indri_lambda = Double.parseDouble(params.get("Indri:lambda"));
			if (params.get("Indri:smoothing").equals("df"))
				Indri_smo = SmoothTech.DF;
			else if (params.get("Indri:smoothing").equals("ctf"))
				Indri_smo = SmoothTech.CTF;

			// validation check
			if (Indri_mu < 0.0 || (Indri_lambda < 0.0 || Indri_lambda > 1.0)
					|| Indri_smo == SmoothTech.NONE) {
				System.err.println("Error: invalid parameters in: Indri");
				System.exit(1);
			}
		} else {
			System.err.println("Error: unidentified retrieval algorithm.");
			System.exit(1);
		}
	}
}
