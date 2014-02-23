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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

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
		dls = new DocLengthStore(READER);

		// read query file
		Map<Integer, String> queries = getQueries(QryParams.queryFilePath);
		List<Integer> sortedQueryID = new ArrayList<Integer>(queries.keySet());
		Collections.sort(sortedQueryID);

		long startTime = System.currentTimeMillis();
		// search!!!
		for (Integer qID : sortedQueryID) {
			QryResult result;

			String qry = queries.get(qID);
			Qryop qTree = QryParser.parseQuery(qry);
			result = qTree.evaluate();

			result.docScores.sortScoresByScore();
			printResults(qID.toString(), result);
		}
		FileOp.writeToFile(QryParams.trecEvalOutPath, resStrBld.toString());
		
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		//System.out.println(totalTime);

	}

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
