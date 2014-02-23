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

		if (QryParams.retrievalAlgm != RetrievalAlgorithm.RANKEDBOOLEAN
				&& QryParams.retrievalAlgm != RetrievalAlgorithm.UNRANKEDBOOLEAN) {
			System.err.println("Error: invalid #or in this alg!");
			System.exit(-1);
		}

		// Seed the result list by evaluating the first query
		// argument. The result could be docScores or invList, depending
		// on the query operator. Wrap a SCORE query operator around it to
		// force it to be a docScores list. There are more efficient ways
		// to do this. This approach is just easy to see and understand.
		Qryop impliedQryOp = new QryopScore(args.get(0));
		QryResult result = impliedQryOp.evaluate();

		for (int i = 1; i < args.size(); i++) {
			impliedQryOp = new QryopScore(args.get(i));
			QryResult iResult = impliedQryOp.evaluate();

			QryResult crntResult = new QryResult();
			int rDoc = 0, iDoc = 0;
			int rSize = result.docScores.scores.size();
			int iSize = iResult.docScores.scores.size();

			while (rDoc < rSize && iDoc < iSize) {
				int rDocID = result.docScores.getDocid(rDoc);
				int iDocID = iResult.docScores.getDocid(iDoc);
				if (rDocID > iDocID) {
					crntResult.docScores.add(iDocID,
							iResult.docScores.getDocidScore(iDoc));
					iDoc++;
				} else if (rDocID < iDocID) {
					crntResult.docScores.add(rDocID,
							result.docScores.getDocidScore(rDoc));
					rDoc++;
				} else {
					crntResult.docScores.add(rDocID, Math.max(
							result.docScores.getDocidScore(rDoc),
							iResult.docScores.getDocidScore(iDoc)));
					rDoc++;
					iDoc++;
				}
			}
			if (rDoc < rSize) {
				while (rDoc < rSize) {
					crntResult.docScores.add(result.docScores.getDocid(rDoc),
							result.docScores.getDocidScore(rDoc));
					rDoc++;
				}
			}
			if (iDoc < iSize) {
				while (iDoc < iSize) {
					crntResult.docScores.add(iResult.docScores.getDocid(iDoc),
							iResult.docScores.getDocidScore(iDoc));
					iDoc++;
				}
			}

			result = crntResult;
		}

		return result;
	}
}
