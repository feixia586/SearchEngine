import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is for getting evaluation from the server, 
 * thus we can do the experiment more efficiently
 * @author fei
 *
 */
public class Script {
	public static List<Integer> fbMu = new ArrayList<Integer>(Arrays.asList(0,
			2500, 5000, 7500, 10000));
	public static List<Integer> fbDocs = new ArrayList<Integer>(Arrays.asList(
			10, 20, 30, 40, 50));
	public static List<Integer> fbTerms = new ArrayList<Integer>(Arrays.asList(
			5, 10, 20, 30, 40, 50));
	public static List<Double> fbOrigWeight = new ArrayList<Double>(
			Arrays.asList(0.0, 0.2, 0.4, 0.6, 0.8, 1.0));

	/**
	 * The line number of MAP for different queries
	 */
	public static List<Integer> mapLineNum = new ArrayList<Integer>(
			Arrays.asList(21, 48, 75, 102, 129, 156, 183, 210, 237, 264, 291,
					318, 345, 372, 399, 426, 453, 480, 507, 534, 561, 588, 615,
					642, 669, 696, 723, 750, 777, 804));

	/**
	 * The line number of P@10, P@20, P@30, MAP for all
	 */
	public static List<Integer> allStat = new ArrayList<Integer>(Arrays.asList(
			849, 851, 852, 832));

	public static void main(String[] args) {
		finalMeasure();
	}
	/**
	 * print final pretty measurement for the experiment
	 */
	public static void finalMeasure() {
		System.out.println("-----------------Final Measure---------------------------");
		String baseResultFile = "baseRes";
		String newResultFile = "newRes";
		
		// run external command to upload the retrieval result to server
		// and get measurement file (newRes) back
		try {
			String command = "sh fetchRes.sh";
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(command);
			pr.waitFor();
		} catch (Exception ex) {
			System.err.println("Error: error in running perl!");
			System.exit(-1);
		}

		// read the baseline measurement file and new measurement file
		String baseContent = FileOp.readFromFile(baseResultFile);
		String newContent = FileOp.readFromFile(newResultFile);

		String[] baseConLines = baseContent.split("\n");
		String[] newConLines = newContent.split("\n");

		// output P@10, P@20, P@30 and map for newRes
		System.out.println(newConLines[allStat.get(0) - 1]);
		System.out.println(newConLines[allStat.get(1) - 1]);
		System.out.println(newConLines[allStat.get(2) - 1]);
		System.out.println(newConLines[allStat.get(3) - 1]);

		// calculate win/loss ratio
		int better_num = 0, worse_num = 0;
		for (int i = 0; i < mapLineNum.size(); i++) {
			int lineNum = mapLineNum.get(i);
			String base_line = baseConLines[lineNum - 1];
			String new_line = newConLines[lineNum - 1];

			String[] baseItems = base_line.split("\t");
			String[] newItems = new_line.split("\t");

			// validation
			if (!baseItems[0].trim().equals("map")
					|| !newItems[0].trim().equals("map")) {
				System.err.println("Error: error in parsing the result file");
				System.exit(-1);
			}

			try {
				double base_value = Double.parseDouble(baseItems[2]);
				double new_value = Double.parseDouble(newItems[2]);
				if (new_value >= base_value) {
					better_num++;
				} else {
					worse_num++;
				}
			} catch (Exception ex) {
				System.err.println("Error: error in parsing the result file");
				System.exit(-1);
			}
		}
		System.out.println("better_num=" + better_num + "\tworse_num=" + worse_num);
		System.out.println("win/loss: " + better_num / (double)worse_num);
		
		// output in a more convenient format
		System.out.println("-----------------Better Format---------------------------");
		System.out.println(newConLines[allStat.get(0) - 1].split("\t")[2]);
		System.out.println(newConLines[allStat.get(1) - 1].split("\t")[2]);
		System.out.println(newConLines[allStat.get(2) - 1].split("\t")[2]);
		System.out.println(newConLines[allStat.get(3) - 1].split("\t")[2]);
		System.out.println(30);
		System.out.println(better_num / (double)worse_num);

		
		
		// delete the newRes file
		try {
		String command = "rm newRes";
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(command);
		pr.waitFor();
		} catch (Exception ex) {
			System.err.println("Error: error in running perl!");
			System.exit(-1);
		}
		
	}
}
