import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

public class QryopUW extends Qryop {
	private int dist;

	public QryopUW(int dist, Qryop... q) {
		this.dist = dist;
		for (int i = 0; i < q.length; i++) {
			this.args.add(q[i]);
		}
	}

	public QryopUW(int dist) {
		this.dist = dist;
	}

	public void add(Qryop a) {
		this.args.add(a);
	}

	public QryResult evaluate() throws IOException {
		List<QryResult> resList = new ArrayList<QryResult>();
		for (int i = 0; i < args.size(); i++) {
			QryResult res = args.get(i).evaluate();
			resList.add(res);
		}

		// init result which will be returned
		QryResult result = new QryResult();
		result.invertedList.field = resList.get(0).invertedList.field;

		// init the docID list
		List<Integer> docIdxs = new ArrayList<Integer>(); 
		for (int i = 0; i < args.size(); i++) docIdxs.add(0); 

		while (true) {
			boolean sameDocFound = true;

			DocPosting postA = resList.get(0).invertedList.postings.get(docIdxs.get(0));
			int docIDA = postA.docid;
			int maxDocID = docIDA, MDID_argIdx = 0;
			
			// check whether it is the same document
			for (int i = 1; i < args.size(); i++) {
				DocPosting postB = resList.get(i).invertedList.postings.get(docIdxs.get(i));
				int docIDB = postB.docid;
				if (docIDA != docIDB)
					sameDocFound = false;

				if (maxDocID < docIDB) {
					maxDocID = docIDB;
					MDID_argIdx = i;
				}
			}
			
			// the same doc hasn't been found
			if (!sameDocFound) {
				// move all the doc index, except the largest one
				if (!moveDocIdxs(resList, docIdxs, MDID_argIdx))
					break;
			} else {
				/* the same doc is found */

				List<DocPosting> posts = getCurrentPosts(resList, docIdxs); 
				List<Integer> locIdxs = new ArrayList<Integer>();
				for (int i = 0; i < args.size(); i++) locIdxs.add(0); 

				DocPosting posting = new DocPosting(maxDocID);
				
				while (true) {
					int min_loc = posts.get(0).positions.get(locIdxs.get(0));
					int	max_loc = posts.get(0).positions.get(locIdxs.get(0));
					int MINL_argIdx = 0;
					
					// find out the max location and min location
					for (int i = 0; i < args.size(); i++) {
						int crnt_loc = posts.get(i).positions.get(locIdxs.get(i));
						max_loc = Math.max(max_loc, crnt_loc);
						
						if (crnt_loc < min_loc) {
							min_loc = crnt_loc;
							MINL_argIdx = i;
						}
					}
					
					// doc found!!
					if (max_loc - min_loc <= dist) {
						posting.add(max_loc);
						// move all the location index
						if (!moveLocIdxs(posts, locIdxs))
							break;
					} else {
						// move the smallest location index
						if (!moveLocIdxs(posts, locIdxs, MINL_argIdx))
							break;
					}
				}
				
				if (posting.tf > 0)
					result.invertedList.add(posting);
				
				// move all the doc index
				if (!moveDocIdxs(resList, docIdxs))
					break;
			}
		}
		
		return result;
	}

	// move the smallest location index
	// return true if the move is OK
	private boolean moveLocIdxs(List<DocPosting> posts, List<Integer> locIdxs, int MINL_argIdx) {
		int new_val = locIdxs.get(MINL_argIdx) + 1;
		if (new_val >= posts.get(MINL_argIdx).tf)
			return false;
		locIdxs.set(MINL_argIdx, new_val);
		
		return true;
	}
	
	// move all the location index
	// return true if the move is OK
	private boolean moveLocIdxs(List<DocPosting> posts, List<Integer> locIdxs) {
		for (int i = 0; i < args.size(); i++) {
			int new_val = locIdxs.get(i) + 1;
			if (new_val >= posts.get(i).tf) {
				return false;
			}

			locIdxs.set(i, new_val);
		}
		
		return true;
	}
	// now we got the same doc, we can get those postings for different args
	private List<DocPosting> getCurrentPosts(List<QryResult> resList, List<Integer> docIdxs) {
		List<DocPosting> posts = new ArrayList<DocPosting>(); 
		
		for (int i = 0; i < args.size(); i++) {
			DocPosting posting = resList.get(i).invertedList.postings.get(docIdxs.get(i));
			posts.add(posting);
		}
		
		return posts;
	}
	
	// move all the docIdxs, except the one corresponding to maxDocID
	// return true if the move is OK
	private boolean moveDocIdxs(List<QryResult> resList, List<Integer> docIdxs, int MDID_argIdx) {
		for (int i = 0; i < args.size(); i++) {
			if (i == MDID_argIdx) continue; 
			
			int new_val = docIdxs.get(i) + 1;
			if (new_val >= resList.get(i).invertedList.df) {
				return false;
			}
			docIdxs.set(i, new_val);
		}
		
		return true;
	}
	
	// move all the docIdxs
	// return true if the move is OK
	private boolean moveDocIdxs(List<QryResult> resList, List<Integer> docIdxs){
		for (int i = 0; i < args.size(); i++) {
			int new_val = docIdxs.get(i) + 1;  
			if (new_val >= resList.get(i).invertedList.df) {
				return false;
			}
			docIdxs.set(i, new_val);
		}
		
		return true;
	}
}
