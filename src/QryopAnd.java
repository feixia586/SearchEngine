/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.DocsAndPositionsEnum;

public class QryopAnd extends Qryop {

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 */
	public QryopAnd(Qryop... q) {
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

		// if retrieval algorithm is Indri, go directly to the Indri evaluation
		if (QryParams.retrievalAlgm == RetrievalAlgorithm.INDRI) {
			return eval_Indri();
		}

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

			// Use the results of the i'th argument to incrementally compute
			// the query operator. Intersection-style query operators
			// iterate over the incremental results, not the results of the
			// i'th query argument.
			int rDoc = 0; /* Index of a document in result. */
			int iDoc = 0; /* Index of a document in iResult. */

			while (rDoc < result.docScores.scores.size()) {

				// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
				// Unranked Boolean AND. Remove from the incremental result
				// any documents that weren't returned by the i'th query
				// argument.

				// Ignore documents matched only by the i'th query arg.
				while ((iDoc < iResult.docScores.scores.size())
						&& (result.docScores.getDocid(rDoc) > iResult.docScores
								.getDocid(iDoc))) {
					iDoc++;
				}

				// If the rDoc document appears in both lists, keep it,
				// otherwise discard it.
				if ((iDoc < iResult.docScores.scores.size())
						&& (result.docScores.getDocid(rDoc) == iResult.docScores
								.getDocid(iDoc))) {

					if (QryParams.retrievalAlgm == RetrievalAlgorithm.UNRANKEDBOOLEAN) {

					} else if (QryParams.retrievalAlgm == RetrievalAlgorithm.RANKEDBOOLEAN) {
						// find out the min score
						float newScore = Math.min(
								result.docScores.getDocidScore(rDoc),
								iResult.docScores.getDocidScore(iDoc));
						result.docScores.setScoreByIndex(rDoc, newScore);
					} else {
						System.err.println("Error: invalid #and in this alg!");
						System.exit(1);
					}
					rDoc++;
					iDoc++;
				} else {
					result.docScores.scores.remove(rDoc);
				}
			}
		}

		return result;
	}

	/**
	 * evaluation for Indri #AND operator
	 * 
	 * @return QryResult which contains score list
	 * @throws IOException
	 */
	public QryResult eval_Indri() throws IOException {
		List<QryResult> resList = new ArrayList<QryResult>();
		for (int i = 0; i < args.size(); i++) {
			Qryop op = new QryopScore(args.get(i));
			resList.add(op.evaluate());
		}

		QryResult result = new QryResult();
		double coeff = 1.0 / args.size();
		int doc_num = QryEval.READER.numDocs();
		
		// validation check
		boolean valid = false;
		for (int i = 0; i < resList.size(); i++) {
			if (resList.get(i).docScores.scores.size() != 0)
				valid = true;
		}
		if (!valid) {
			//System.err.println("Args in Indri #AND should have some results!"); 
			return result;
		}

		// go through all the documents, note the score is in log space, so we
		// can directly add them
		for (int id = 0; id < doc_num; id++) {
			double tmp = 0;
			for (int idx = 0; idx < resList.size(); idx++) {
				// Note: the docScores.scores.size() shouldn't be always zero
				if (resList.get(idx).docScores.scores.size() == 0) continue; 

				tmp += resList.get(idx).docScores.getDocidScore(id);
			}
			result.docScores.add(id, (float) (coeff * tmp));
		}

		return result;
	}
}
