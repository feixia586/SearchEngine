/*
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.PriorityQueue;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

public class QryEval {

	static String usage = "Usage:  java "
			+ System.getProperty("sun.java.command") + " paramFile\n\n";

	/**
	 * The index file reader is accessible via a global variable. This isn't
	 * great programming style, but the alternative is for every query operator
	 * to store or pass this value, which creates its own headaches.
	 */
	public static IndexReader READER;

	public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
			Version.LUCENE_43);
	static {
		analyzer.setLowercase(true);
		analyzer.setStopwordRemoval(true);
		analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
	}

	public static DocLengthStore dls;

	private static StringBuilder resStrBld = new StringBuilder();

	/**
	 * @param args
	 *            The only argument should be one file name, which indicates the
	 *            parameter file.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// must supply parameter file
		if (args.length < 1) {
			System.err.println(usage);
			System.exit(1);
		}

		// parse parameter File
		QryParams.parseParameterFile(args[0]);

		// open the index
		READER = DirectoryReader.open(FSDirectory.open(new File(
				QryParams.indexPath)));

		if (READER == null) {
			System.err.println(usage);
			System.exit(1);
		}

		// init the dls that stores the doc length information
		dls = new DocLengthStore(READER);

		// read query file
		Map<Integer, String> queries = getQueries(QryParams.queryFilePath);
		List<Integer> sortedQueryID = new ArrayList<Integer>(queries.keySet());
		Collections.sort(sortedQueryID);

		long startTime = System.currentTimeMillis();

		// read the feedback initial ranking file, might be used later
		Map<Integer, QryResult> initRankResult = null;
		if (QryParams.QE_fb && QryParams.QE_fbInitialRankingFile != null) {
			initRankResult = readResultFromFile(QryParams.QE_fbInitialRankingFile);
		}
		// the expanded queries, might be used later
		StringBuilder expandedQries = new StringBuilder();
		StringBuilder combinedQries = new StringBuilder();

		// begin search!!!
		for (Integer qID : sortedQueryID) {
			QryResult result;

			String qry = queries.get(qID);
			if (QryParams.QE_fb == false) {
				Qryop qTree = QryParser.parseQuery(qry);
				result = qTree.evaluate();
			} else {
				if (QryParams.QE_fbInitialRankingFile != null) {
					assert (initRankResult != null);
					result = initRankResult.get(qID);

				} else {
					Qryop qTree = QryParser.parseQuery(qry);
					result = qTree.evaluate();
				}

				QryExpander qryExpander = new QryExpander();
				// get expanded query
				String Ex_qry = qryExpander.expandQuery(result);
				expandedQries.append(qID + ": " + Ex_qry + "\n");

				// get combined query
				String Com_qry = qryExpander.combineQuery(qry, Ex_qry);
				combinedQries.append(qID + ": " + Com_qry + "\n");

				Qryop qTree = QryParser.parseQuery(Com_qry);
				result = qTree.evaluate();
			}

			result = refineResult(result, 100);
			// result.docScores.sortScoresByScore();

			printResults(qID.toString(), result);
		}
		FileOp.writeToFile(QryParams.trecEvalOutPath, resStrBld.toString());
		if (QryParams.QE_fb && QryParams.QE_fbExpansionQueryFile != null) {
			FileOp.writeToFile(QryParams.QE_fbExpansionQueryFile,
					expandedQries.toString());
			//FileOp.writeToFile(QryParams.QE_fbExpansionQueryFile + "com",
			//		combinedQries.toString());
		}

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println(totalTime);
		
		Script.finalMeasure();
	}

	/**
	 * Refine the results, get the top documents using priority_queue. Note:
	 * They are ranked in descending order!
	 * 
	 * @param result
	 *            the original result
	 * @param n
	 *            the number of document that would be returned at most
	 * @return the refined result which contains at most n documents
	 */

	static QryResult refineResult(QryResult result, int n) {
		QryResult new_result = new QryResult();

		int size = Math.min(n, result.docScores.scores.size());

		if (size <= 0)
			return new_result;

		Comparator<ScoreListEntry> comp = new ScoreComparator();
		PriorityQueue<ScoreListEntry> queue = new PriorityQueue<ScoreListEntry>(
				size, comp);

		List<ScoreListEntry> lt = result.docScores.scores;
		for (int i = 0; i < size; i++) {
			queue.add(lt.get(i));
		}

		int total_item = lt.size();
		for (int i = size; i < total_item; i++) {
			if (comp.compare(lt.get(i), queue.peek()) > 0) {
				queue.poll();
				queue.add(lt.get(i));
			}
		}

		// use a stack to reverse the order
		Stack<ScoreListEntry> stk = new Stack<ScoreListEntry>();
		while (queue.size() != 0) {
			stk.push(queue.poll());
		}
		while (stk.size() != 0) {
			new_result.docScores.add_entry(stk.pop());
		}

		return new_result;
	}

	/**
	 * Read a document ranking from the feedback initial ranking file.
	 * 
	 * @param filepath
	 *            the file path
	 * @return a HashMap from queryID to QryResult
	 * @throws Exception
	 */
	static Map<Integer, QryResult> readResultFromFile(String filepath)
			throws Exception {
		Map<Integer, QryResult> initRankResult = new HashMap<Integer, QryResult>();
		Map<Integer, ArrayList<String>> queryResStr = new HashMap<Integer, ArrayList<String>>();

		String content = FileOp.readFromFile(filepath);
		String[] lines = content.split("\n");

		// construct the queryResStr
		for (int i = 0; i < lines.length; i++) {
			int idx = lines[i].indexOf(" ");
			int queryID = Integer.parseInt(lines[i].substring(0, idx));
			String resultStr = lines[i].substring(idx + 1).trim();
			if (queryResStr.get(queryID) == null) {
				queryResStr.put(queryID, new ArrayList<String>());
				queryResStr.get(queryID).add(resultStr);
			} else {
				queryResStr.get(queryID).add(resultStr);
			}
		}

		// construct the initRankResult based on queryResStr
		for (Integer key : queryResStr.keySet()) {
			ArrayList<String> resList = queryResStr.get(key);
			QryResult result = new QryResult();
			for (int i = 0; i < resList.size(); i++) {
				String[] items = resList.get(i).split(" ");
				String externalID = items[1];
				int internalID = getInternalDocid(READER, externalID);
				double score = Double.parseDouble(items[3]);
				result.docScores.add(internalID, (float) score);
			}
			initRankResult.put(key, result);
		}

		return initRankResult;
	}

	/**
	 * read the query file and store queries to a map data structure
	 * 
	 * @param queryFilePath
	 *            the path of query file
	 * @return the map that stores those queries, map[query_id]->query string
	 */
	static Map<Integer, String> getQueries(String queryFilePath) {
		Map<Integer, String> queries = new HashMap<Integer, String>();
		try {
			Scanner scan = new Scanner(new File(queryFilePath));
			String line = null;
			do {
				line = scan.nextLine();
				int index = line.indexOf(':');
				Integer queryID = Integer.parseInt(line.substring(0, index));
				String queryContent = line.substring(index + 1);
				queries.put(queryID, queryContent);
			} while (scan.hasNext());

			scan.close();
		} catch (FileNotFoundException ex) {
			System.err.println("Error: cannot open file: " + queryFilePath);
			System.exit(1);
		}

		return queries;
	}

	/**
	 * Get the external document id for a document specified by an internal
	 * document id. Ordinarily this would be a simple call to the Lucene index
	 * reader, but when the index was built, the indexer added "_0" to the end
	 * of each external document id. The correct solution would be to fix the
	 * index, but it's too late for that now, so it is fixed here before the id
	 * is returned.
	 * 
	 * @param iid
	 *            The internal document id of the document.
	 * @throws IOException
	 */
	static String getExternalDocid(int iid) throws IOException {
		Document d = QryEval.READER.document(iid);
		String eid = d.get("externalId");

		if ((eid != null) && eid.endsWith("_0"))
			eid = eid.substring(0, eid.length() - 2);

		return (eid);
	}

	/**
	 * Finds the internal document id for a document specified by its external
	 * id, e.g. clueweb09-enwp00-88-09710. If no such document exists, it throws
	 * an exception. Note that the lucene indexer added "_0" to the end of each
	 * external document id, so that suffix is added to the externalId before
	 * the internal id is fetched.
	 * 
	 * @param externalId
	 * @return Internal doc id of lucene suitable for finding document vectors
	 *         etc.
	 * @throws Exception
	 */
	static int getInternalDocid(IndexReader READER, String externalId)
			throws Exception {

		// add _0 to account for how ids were indexed by lucene indexer
		Query q = new TermQuery(new Term("externalId", externalId + "_0"));

		IndexSearcher searcher = new IndexSearcher(READER);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		if (hits.length < 1) {
			throw new Exception("External id not found.");
		} else {
			return hits[0].doc;
		}
	}

	/**
	 * Print the query results.
	 * 
	 * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
	 * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
	 * 
	 * QueryID Q0 DocID Rank Score RunID
	 * 
	 * @param queryID
	 *            Original query ID.
	 * @param result
	 *            Result object generated by {@link Qryop#evaluate()}.
	 * @throws IOException
	 */
	static void printResults(String queryID, QryResult result)
			throws IOException {

		// System.out.println(queryID + ":  ");
		if (result.docScores.scores.size() < 1) {
			System.out.println(queryID + "\tQ0\tdummy\t1\t0\trun-1");
		} else {
			for (int i = 0; i < result.docScores.scores.size(); i++) {
				if (i >= 100)
					break;

				StringBuilder strBld = new StringBuilder(queryID);
				strBld.append("\tQ0\t")
						.append(getExternalDocid(result.docScores.getDocid(i)))
						.append("\t").append((i + 1)).append("\t")
						.append(result.docScores.getDocidScore(i))
						.append("\trun-1");
				resStrBld.append(strBld).append("\r\n");
				System.out.println(strBld.toString());
			}
		}
	}

	/**
	 * Given a query string, returns the terms one at a time with stopwords
	 * removed and the terms stemmed using the Krovetz stemmer.
	 * 
	 * Use this method to process raw query terms.
	 * 
	 * @param query
	 *            String containing query
	 * @return Array of query tokens
	 * @throws IOException
	 */
	public static String[] tokenizeQuery(String query) throws IOException {

		TokenStreamComponents comp = analyzer.createComponents("dummy",
				new StringReader(query));
		TokenStream tokenStream = comp.getTokenStream();

		CharTermAttribute charTermAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		List<String> tokens = new ArrayList<String>();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			tokens.add(term);
		}
		return tokens.toArray(new String[tokens.size()]);
	}
}

// DocLengthStore s = new DocLengthStore(READER);
//
// /*
// * The code below is an unorganized set of examples that show you
// * different ways of accessing the index. Some of these are only useful
// * in HW2 or HW3.
// */
//
// // Lookup the document length of the body field of doc 0.
// System.out.println(s.getDocLength("body", 0));
//
// // How to use the term vector.
// //TermVector tv = new TermVector(1, "body");
// //System.out.println(tv.stemString(100)); // get the string for the 100th
// // // stem
// //System.out.println(tv.stemDf(100)); // get its df
// //System.out.println(tv.totalStemFreq(100)); // get its ctf
//
// /**
// * The index is open. Start evaluating queries. The examples below show
// * the query tree that should be created for each query.
// *
// * The general pattern is to tokenize the query term (so that it gets
// * converted to lowercase, stopped, stemmed, etc), create a Term node to
// * fetch the inverted list, create a Score node to convert an inverted
// * list to a score list, evaluate the query, and print results.
// *
// * Modify this section so that you read a query from a file, parse it,
// * and form the query tree automatically.
// */
//
// // A one-word query.
// printResults("pea", (new QryopScore(new QryopTerm(
// tokenizeQuery("a")[0]))).evaluate());
//
// // A two-word query.
// printResults("#AND (broccoli cauliflower)", (new QryopAnd(
// new QryopTerm(tokenizeQuery("broccoli")[0]), new QryopTerm(
// tokenizeQuery("cauliflower")[0]))).evaluate());
//
// // A more complex query.
// printResults("#AND (aparagus broccoli cauliflower #SYN(peapods peas))",
// (new QryopAnd(new QryopTerm(tokenizeQuery("asparagus")[0]),
// new QryopTerm(tokenizeQuery("broccoli")[0]),
// new QryopTerm(tokenizeQuery("cauliflower")[0]),
// new QryopSyn(
// new QryopTerm(tokenizeQuery("peapods")[0]),
// new QryopTerm(tokenizeQuery("peas")[0]))))
// .evaluate());
//
// // A different way to create the previous query. This doesn't use
// // a stack, but it may make it easier to see how you would parse a
// // query with a stack-based architecture.
// Qryop op1 = new QryopAnd();
// op1.add(new QryopTerm(tokenizeQuery("asparagus")[0]));
// op1.add(new QryopTerm(tokenizeQuery("broccoli")[0]));
// op1.add(new QryopTerm(tokenizeQuery("cauliflower")[0]));
// Qryop op2 = new QryopSyn();
// op2.add(new QryopTerm(tokenizeQuery("peapods")[0]));
// op2.add(new QryopTerm(tokenizeQuery("peas")[0]));
// op1.add(op2);
// printResults("#AND (aparagus broccoli cauliflower #SYN(peapods peas))",
// op1.evaluate());
//
// // Using the example query parser. Notice that this does no
// // lexical processing of query terms. Add that to the query
// // parser.
// Qryop qTree;
// qTree = parseQuery("apple.test pie-boy");
// printResults("apple pie", qTree.evaluate());
//
// /*
// * Create the trec_eval output. Your code should write to the file
// * specified in the parameter file, and it should write the results that
// * you retrieved above. This code just allows the testing infrastructure
// * to work on QryEval.
// */
// BufferedWriter writer = null;
//
// try {
// writer = new BufferedWriter(new FileWriter(new File("teval.in")));
//
// writer.write("1 Q0 clueweb09-enwp01-75-20596 1 1.0 run-1");
// writer.write("1 Q0 clueweb09-enwp01-58-04573 2 0.9 run-1");
// writer.write("1 Q0 clueweb09-enwp01-24-11888 3 0.8 run-1");
// writer.write("2 Q0 clueweb09-enwp00-70-20490 1 0.9 run-1");
// } catch (Exception e) {
// e.printStackTrace();
// } finally {
// try {
// writer.close();
// } catch (Exception e) {
// }
// }
//
