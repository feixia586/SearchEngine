/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

public class QryopScore extends Qryop {

	/**
	 * The SCORE operator accepts just one argument.
	 */
	public QryopScore(Qryop q) {
		this.args.add(q);
	}

	/**
	 * Allow a SCORE operator to be created with no arguments. This simplifies
	 * the design of some query parsing architectures.
	 */
	public QryopScore() {
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

		// Evaluate the query argument.
		QryResult result = args.get(0).evaluate();

		// Each pass of the loop computes a score for one document. Note: If the
		// evaluate operation
		// above returned a score list (which is very possible), this loop gets
		// skipped.
		if (QryParams.retrievalAlgm == RetrievalAlgorithm.UNRANKEDBOOLEAN) {
			for (int i = 0; i < result.invertedList.df; i++) {

				// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
				// Unranked Boolean. All matching documents get a score of 1.0.
				result.docScores.add(result.invertedList.postings.get(i).docid,
						(float) 1.0);
			}
		} else if (QryParams.retrievalAlgm == RetrievalAlgorithm.RANKEDBOOLEAN) {
			for (int i = 0; i < result.invertedList.df; i++){
				result.docScores.add(result.invertedList.postings.get(i).docid,
						result.invertedList.postings.get(i).tf);
			}
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.
		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}
}
