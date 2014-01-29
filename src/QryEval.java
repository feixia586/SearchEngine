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
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
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

  static String usage = "Usage:  java " + System.getProperty("sun.java.command")
      + " paramFile\n\n";

  /**
   * The index file reader is accessible via a global variable. This
   * isn't great programming style, but the alternative is for every
   * query operator to store or pass this value, which creates its own
   * headaches.
   */
  public static IndexReader READER;

  public static EnglishAnalyzerConfigurable analyzer =
      new EnglishAnalyzerConfigurable (Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }

  /**
   * @param args The only argument should be one file name, which
   * indicates the parameter file.
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    
    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }

    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    
    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }

    DocLengthStore s = new DocLengthStore(READER);

    /*
     *  The code below is an unorganized set of examples that show
     *  you different ways of accessing the index.  Some of these
     *  are only useful in HW2 or HW3.
     */

    // Lookup the document length of the body field of doc 0.
    System.out.println(s.getDocLength("body", 0));

    // How to use the term vector.
    TermVector tv = new TermVector(1, "body");
    System.out.println(tv.stemString(100)); // get the string for the 100th stem
    System.out.println(tv.stemDf(100)); // get its df
    System.out.println(tv.totalStemFreq(100)); // get its ctf
    
    /**
     *  The index is open. Start evaluating queries. The examples below
     *  show the query tree that should be created for each query.
     *
     *  The general pattern is to tokenize the  query term (so that it
     *  gets converted to lowercase, stopped, stemmed, etc), create a
     *  Term node to fetch the inverted list, create a Score node to
     *  convert an inverted list to a score list, evaluate the query,
     *  and print results.
     * 
     *  Modify this section so that you read a query from a file,
     *  parse it, and form the query tree automatically.
     */

    //  A one-word query.
    printResults("pea",
        (new QryopScore(
    	     new QryopTerm(tokenizeQuery("pea")[0]))).evaluate());

    //  A two-word query.
    printResults("#AND (broccoli cauliflower)",
        (new QryopAnd(
            new QryopTerm(tokenizeQuery("broccoli")[0]),
            new QryopTerm(tokenizeQuery("cauliflower")[0]))).evaluate());


    //  A more complex query.
    printResults("#AND (aparagus broccoli cauliflower #SYN(peapods peas))",
        (new QryopAnd(
            new QryopTerm(tokenizeQuery("asparagus")[0]),
            new QryopTerm(tokenizeQuery("broccoli")[0]),
            new QryopTerm(tokenizeQuery("cauliflower")[0]),
            new QryopSyn(
                new QryopTerm(tokenizeQuery("peapods")[0]), 
                new QryopTerm(tokenizeQuery("peas")[0])))).evaluate());

    //  A different way to create the previous query.  This doesn't use
    //  a stack, but it may make it easier to see how you would parse a
    //  query with a stack-based architecture.
    Qryop op1 = new QryopAnd();
    op1.add (new QryopTerm(tokenizeQuery("asparagus")[0]));
    op1.add (new QryopTerm(tokenizeQuery("broccoli")[0]));
    op1.add (new QryopTerm(tokenizeQuery("cauliflower")[0]));
    Qryop op2 = new QryopSyn();
    op2.add (new QryopTerm(tokenizeQuery("peapods")[0]));
    op2.add (new QryopTerm(tokenizeQuery("peas")[0]));
    op1.add (op2);
    printResults("#AND (aparagus broccoli cauliflower #SYN(peapods peas))",
		 op1.evaluate());

    //  Using the example query parser.  Notice that this does no
    //  lexical processing of query terms.  Add that to the query
    //  parser.
    Qryop qTree;
    qTree = parseQuery ("apple pie");
    printResults ("apple pie", qTree.evaluate ());

    /*
     *  Create the trec_eval output.  Your code should write to the
     *  file specified in the parameter file, and it should write the
     *  results that you retrieved above.  This code just allows the
     *  testing infrastructure to work on QryEval.
     */
    BufferedWriter writer = null;

    try {
      writer = new BufferedWriter (new FileWriter (new File ("teval.in")));

      writer.write("1 Q0 clueweb09-enwp01-75-20596 1 1.0 run-1");
      writer.write("1 Q0 clueweb09-enwp01-58-04573 2 0.9 run-1");
      writer.write("1 Q0 clueweb09-enwp01-24-11888 3 0.8 run-1");
      writer.write("2 Q0 clueweb09-enwp00-70-20490 1 0.9 run-1");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
	writer.close();
      } catch (Exception e) {
      }
    }

  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id.  Ordinarily this would be a simple call to
   *  the Lucene index reader, but when the index was built, the
   *  indexer added "_0" to the end of each external document id.  The
   *  correct solution would be to fix the index, but it's too late
   *  for that now, so it is fixed here before the id is returned.
   *
   * @param iid The internal document id of the document.
   * @throws IOException 
   */
  static String getExternalDocid (int iid) throws IOException {
    Document d = QryEval.READER.document (iid);
    String eid = d.get ("externalId");

    if ((eid != null) && eid.endsWith ("_0"))
      eid = eid.substring (0, eid.length()-2);

    return (eid);
  }

  /**
   *  parseQuery converts a query string into a query tree.
   * @param qString A string containing a query.
   * @param qTree A query tree
   * @throws IOException 
   */
  static Qryop parseQuery (String qString) throws IOException {

    Qryop currentOp = null;
    Stack<Qryop> stack = new Stack<Qryop>();

    /*
     *  Add a default query operator to an unstructured query.  This
     *  is a tiny bit easier if unnecessary whitespace is removed.
     */
    qString = qString.trim ();

    if (qString.charAt (0) != '#') {
      qString = "#or(" + qString + ")";
    }

    /*
     *  Tokenize the query.  
     */
    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;

    /*
     *  Each pass of the loop processes one token.  To improve
     *  efficiency and clarity, the query operator on the top of the
     *  stack is also stored in currentOp.
     */
    while (tokens.hasMoreTokens()) {

      token = tokens.nextToken();

      if (token.matches ("[ ,(\t\n\r]"))	// Ignore most delimiters.
	  ;
      else if (token.equalsIgnoreCase ("#and")) {
	currentOp = new QryopAnd ();
	stack.push (currentOp);
      }
      else if (token.equalsIgnoreCase ("#syn")) {
	currentOp = new QryopSyn ();
	stack.push (currentOp);
      }
      else if (token.startsWith (")")) {	// Finish current query operator.
	/*
	 *  If the current query operator is not an argument to
	 *  another query operator (i.e., the stack is empty when it
	 *  is removed), we're done (assuming correct syntax - see
	 *  below).  Otherwise, add the current operator as an
	 *  argument to the higher-level operator, and shift
	 *  processing back to the higher-level operator.
	 */
	stack.pop ();

	if (stack.empty ())
	  break;
	      
	Qryop arg = currentOp;
	currentOp = stack.peek ();
	currentOp.add (arg);
      }
      else {

	//  NOTE: You should do lexical processing of the token before
	//  creating the query term, and you should check to see whether
	//  the token specifies a particular field (e.g., apple.title).
	currentOp.add (new QryopTerm (token));
      };
    }

    /*
     *  A broken structured query can leave unprocessed tokens on the
     *  stack, so check for that.
     */
    if (tokens.hasMoreTokens()) {
	System.err.println ("Error:  Query syntax is incorrect.  " + qString);
	return null;
    }

    return currentOp;
  }

  /**
   * Print the query results. 
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT.  YOU MUST CHANGE THIS
   * METHOD SO THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK
   * PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName Original query.
   * @param result Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException 
   */
  static void printResults(String queryName, QryResult result) throws IOException {

    System.out.println(queryName + ":  ");
    if (result.docScores.scores.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      for (int i = 0; i < result.docScores.scores.size(); i++) {
        System.out.println("\t" + i + ":  "
			   + getExternalDocid (result.docScores.getDocid(i))
			   + ", "
			   + result.docScores.getDocidScore(i));
      }
    }
  }

  /**
   * Given a query string, returns the terms one at a time with stopwords
   * removed and the terms stemmed using the Krovetz stemmer. 
   * 
   * Use this method to process raw query terms. 
   * 
   * @param query String containing query
   * @return Array of query tokens
   * @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {
    
    TokenStreamComponents comp =
	analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute =
	tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();
    
    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }
}
