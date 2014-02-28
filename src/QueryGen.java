import java.util.ArrayList;
import java.util.List;

public class QueryGen {
	/*public static void main(String[] args) {
		List<Double> w = new ArrayList<Double>();
		w.add(0.0); w.add(0.3); w.add(0.3); w.add(0.0); w.add(0.4);
		gen_wsum_diffRep("queries_raw.txt", w);
		
		//gen_sdm_queries("queries_raw.txt");
	}*/

	/**
	 * Generate queries for
	 * @param filename
	 * @param w
	 */
	public static void gen_wsum_diffRep(String filename, List<Double> w) {
		StringBuilder sb = new StringBuilder();

		String content = FileOp.readFromFile(filename);
		String[] lines = content.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String[] parts = lines[i].split(":");
			String query = parts[1];
			String[] terms = query.split(" ");
			
			sb.append(parts[0]); sb.append(":#AND(");
			for (int j = 0; j < terms.length; j++) {
				sb.append("#WSUM(");
				sb.append(w.get(0) + " "); sb.append(terms[j] + ".url "); 
				sb.append(w.get(1) + " "); sb.append(terms[j] + ".keywords "); 
				sb.append(w.get(2) + " "); sb.append(terms[j] + ".title "); 
				sb.append(w.get(3) + " "); sb.append(terms[j] + ".inlink "); 
				sb.append(w.get(4) + " "); sb.append(terms[j] + ".body) "); 
			}
			sb.append(")");
			//sb.append(System.lineSeparator());
			sb.append("\n");
		}
		FileOp.writeToFile("queries_gen.txt", sb.toString());
	}
	
	public static void gen_sdm_queries(String filename) {
		StringBuilder sb = new StringBuilder();

		String content = FileOp.readFromFile(filename);
		String[] lines = content.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String[] parts = lines[i].split(":");
			String query = parts[1];
			String[] terms = query.split(" ");
			
			sb.append(parts[0]); sb.append(":#WEIGHT(");
			
			// first sub-query
			sb.append("#AND("); sb.append(query); sb.append(") ");
			
			// second sub-query
			sb.append("#AND(");
			for (int j = 0; j < terms.length-1; j++) {
				sb.append("#NEAR/1("); 
				sb.append(terms[j] + " " + terms[j+1] + ") ");
			}
			sb.append(") ");
			
			// third sub-query
			sb.append("#AND("); 
			for(int j = 0; j < terms.length-1; j++) {
				sb.append("#UW/8(");
				sb.append(terms[j] + " " + terms[j+1]+ ") ");
			}
			sb.append(") ");
			
			sb.append(")");
			//sb.append(System.lineSeparator());
			sb.append("\n");
		}
		FileOp.writeToFile("queries_gen.txt", sb.toString());
	}
}
