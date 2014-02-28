import java.io.IOException;
import java.util.List;
import java.util.ArrayList;


public class QryopWSUM extends Qryop {
	// weight parameters
	private List<Double> weight = new ArrayList<Double>();

	// coeff = weight.get(i) / wsum
	private List<Double> coeff = new ArrayList<Double>();  
	
	// wsum is the sum of all the weight parameters
	private double wsum = 0;

	public QryopWSUM(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}
	
	public void add(Qryop a) {
		this.args.add(a);
	}
	
	public void add_weight(double w) {
		this.weight.add(w);
		wsum += w;
	}
	
	public QryResult evaluate() throws IOException {
		// assertion
		assert(args.size() == weight.size());

		List<QryResult> resList = new ArrayList<QryResult>();
		for (int i = 0; i < args.size(); i++) {
			Qryop op = new QryopScore(args.get(i));
			resList.add(op.evaluate());
		}
		
		// calculate the coefficients
		coeff.clear();
		for (int i = 0; i < weight.size(); i++)
			coeff.add(weight.get(i) / wsum);

		QryResult result = new QryResult();
		int doc_num = QryEval.READER.numDocs();
		for (int id = 0; id < doc_num; id++) {
			double score = 0;
			for (int idx = 0; idx < resList.size(); idx++) {
				score += coeff.get(idx) * Math.exp(resList.get(idx).docScores.getDocidScore(id));
			}
			
			// Remember the score of Indri should be in log space
			result.docScores.add(id, (float) Math.log(score));
		}

		return result;
	}
}
