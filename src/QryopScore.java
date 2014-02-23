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
			for (int i = 0; i < result.invertedList.df; i++) {
				result.docScores.add(result.invertedList.postings.get(i).docid,
						result.invertedList.postings.get(i).tf);
			}
		} else if (QryParams.retrievalAlgm == RetrievalAlgorithm.BM25) {
			String field = result.invertedList.field;
			int N = QryEval.READER.getDocCount(field);
			int df = result.invertedList.df;
			int qtf = 1;
			double avg_doclen = QryEval.READER.getSumTotalTermFreq(field)
					/ (double) N;
			double idf = Math.log((N - df + 0.5) / (df + 0.5));
			double user_weights = (QryParams.BM25_k3 + 1) * qtf
					/ (double) (QryParams.BM25_k3 + qtf);
			for (int i = 0; i < df; i++) {
				DocPosting posting = result.invertedList.postings.get(i);
				int tf = posting.tf;
				long doclen = QryEval.dls.getDocLength(field, posting.docid);
				double final_tf = tf
						/ (double) (tf + QryParams.BM25_k1
								* (1 - QryParams.BM25_b + QryParams.BM25_b
										* doclen / avg_doclen));
				double doc_score = idf * final_tf * user_weights;
				result.docScores.add(posting.docid, (float) doc_score);
			}

		} else if (QryParams.retrievalAlgm == RetrievalAlgorithm.INDRI) {
			String field = result.invertedList.field;

			double smo = 0;
			if (QryParams.Indri_smo == SmoothTech.DF) {
				int df = result.invertedList.df;
				int len_docs = QryEval.READER.getDocCount(field);
				smo = df / (double) len_docs;
			} else if (QryParams.Indri_smo == SmoothTech.CTF) {
				int ctf = result.invertedList.ctf;
				long len_terms = QryEval.READER.getSumTotalTermFreq(field);
				smo = ctf / (double) len_terms;
			} else {
				System.err.println("Error: param Indri_smo invalid!");
				System.exit(1);
			}

			int df = result.invertedList.df;
			// actually, doc_num = QryEval.READER.getDocCount(field);
			int doc_num = QryEval.READER.numDocs();
			int id = 0, p = 0;
			while (id < doc_num || p < df) {
				if (p < df) {
					DocPosting post = result.invertedList.postings.get(p);
					int pid = post.docid;
					if (id == pid) {
						long doc_len = QryEval.dls.getDocLength(field, id);
						float score = calc_score(post.tf, smo, doc_len);
						result.docScores.add(id, score);
						p++; id++;
						continue;
					}
				}
				
				float score = calc_score(0, smo, QryEval.dls.getDocLength(field, id));
				result.docScores.add(id, score);
				id++;
			}
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.
		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	public float calc_score(int tf, double smo, long doc_len) {
		double part1 = (QryParams.Indri_lambda * (tf + QryParams.Indri_mu * smo)
				/ (doc_len + QryParams.Indri_mu));
		double part2 = ((1 - QryParams.Indri_lambda) * smo); 
		float res = (float) Math.log(part1 + part2);
		return res;
	}
}
