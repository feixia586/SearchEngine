
import java.io.IOException;

/***
 * @author Fei Xia
 * 
 */
public class QryopOr extends Qryop {
	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 */
	public QryopOr(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluate the query operator.
	 */
	public QryResult evaluate() throws IOException {

		// Seed the result list by evaluating the first query
		// argument. The result could be docScores or invList, depending
		// on the query operator. Wrap a SCORE query operator around it to
		// force it to be a docScores list. There are more efficient ways
		// to do this. This approach is just easy to see and understand.
		Qryop impliedQryOp = new QryopScore(args.get(0));
		QryResult result = impliedQryOp.evaluate();

		// Each pass of the loop evaluates one query argument.
		for (int i = 1; i < args.size(); i++) {

			impliedQryOp = new QryopScore(args.get(i));
			QryResult iResult = impliedQryOp.evaluate();

			int iSize = iResult.docScores.scores.size();
			if (QryParams.retrievalAlgm == RetrievalAlgorithm.UNRANKEDBOOLEAN){
				for (int j = 0; j < iSize; j++) {
					int iDocID = iResult.docScores.getDocid(j);
					if (result.docScores.containsDocID(iDocID) < 0)
						result.docScores.add(iDocID, (float) 1.0);
				}
			} else if (QryParams.retrievalAlgm == RetrievalAlgorithm.RANKEDBOOLEAN) {
				for (int j = 0; j < iSize; j++) {
					int iDocID = iResult.docScores.getDocid(j);
					int idx = result.docScores.containsDocID(iDocID);
					if (idx < 0) { 
						result.docScores.add(iDocID, iResult.docScores.getDocidScore(j));
					} else {
						result.docScores.setScoreByIndex(idx, 
								Math.max(result.docScores.getDocidScore(idx), 
										iResult.docScores.getDocidScore(j)));;
					}
						
				}
			}
		}
		
		result.docScores.sortScoresByDocID();

		return result;
	}
}
