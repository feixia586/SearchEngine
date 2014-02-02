import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Stack;

public class QryParser {
	/**
	 * parseQuery converts a query string into a query tree.
	 * 
	 * @param qString
	 *            A string containing a query.
	 * @param qTree
	 *            A query tree
	 * @throws IOException
	 */
	public static Qryop parseQuery(String qString) throws IOException {

		Qryop currentOp = null;
		Stack<Qryop> stack = new Stack<Qryop>();

		/*
		 * Add a default query operator to an unstructured query. This is a tiny
		 * bit easier if unnecessary whitespace is removed.
		 */
		qString = qString.trim();

		if (qString.charAt(0) != '#') {
			qString = "#and(" + qString + ")";
		}

		/*
		 * Tokenize the query.
		 */
		StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()",
				true);
		String token = null;

		/*
		 * Each pass of the loop processes one token. To improve efficiency and
		 * clarity, the query operator on the top of the stack is also stored in
		 * currentOp.
		 */
		while (tokens.hasMoreTokens()) {

			token = tokens.nextToken();

			if (token.matches("[ ,(\t\n\r]")) // Ignore most delimiters.
				;
			else if (token.equalsIgnoreCase("#and")) {
				currentOp = new QryopAnd();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#syn")) {
				currentOp = new QryopSyn();
				stack.push(currentOp);
			} else if (token.startsWith(")")) { // Finish current query
												// operator.
				/*
				 * If the current query operator is not an argument to another
				 * query operator (i.e., the stack is empty when it is removed),
				 * we're done (assuming correct syntax - see below). Otherwise,
				 * add the current operator as an argument to the higher-level
				 * operator, and shift processing back to the higher-level
				 * operator.
				 */
				stack.pop();

				if (stack.empty())
					break;

				Qryop arg = currentOp;
				currentOp = stack.peek();
				currentOp.add(arg);
			} else {

				// NOTE: You should do lexical processing of the token before
				// creating the query term, and you should check to see whether
				// the token specifies a particular field (e.g., apple.title).
				currentOp.add(new QryopTerm(token));
			}
			;
		}

		/*
		 * A broken structured query can leave unprocessed tokens on the stack,
		 * so check for that.
		 */
		if (tokens.hasMoreTokens()) {
			System.err
					.println("Error:  Query syntax is incorrect.  " + qString);
			return null;
		}

		return currentOp;
	}
}
