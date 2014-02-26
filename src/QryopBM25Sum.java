import java.io.IOException;

public class QryopBM25Sum extends Qryop {
	public QryopBM25Sum(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	public void add(Qryop a) {
		this.args.add(a);
	}

	public QryResult evaluate() throws IOException {
		Qryop impliedQryop = new QryopScore(args.get(0));
		QryResult result = impliedQryop.evaluate();
		
		if (QryParams.retrievalAlgm != RetrievalAlgorithm.BM25) {
			System.err.println("Error: invaild #sum in this alg!");
			System.exit(1);
		}

		for (int i = 0; i < args.size(); i++) {
			impliedQryop = new QryopScore(args.get(i));
			QryResult iResult = impliedQryop.evaluate();

			QryResult crntResult = new QryResult();
			int rDoc = 0, iDoc = 0;
			int rSize = result.docScores.scores.size();
			int iSize = iResult.docScores.scores.size();

			while (rDoc < rSize && iDoc < iSize) {
				int rDocID = result.docScores.getDocid(rDoc);
				int iDocID = iResult.docScores.getDocid(iDoc);

				float rScore = result.docScores.getDocidScore(rDoc);
				float iScore = iResult.docScores.getDocidScore(iDoc);
				if (rDocID > iDocID) {
					crntResult.docScores.add(iDocID, iScore);
					iDoc++;
				} else if (rDocID < iDocID) {
					crntResult.docScores.add(rDocID, rScore);
					rDoc++;
				} else {
					crntResult.docScores.add(rDocID, iScore + rScore);
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
