
import java.io.IOException;

/***
 * 
 * @author Fei Xia
 * 
 */
public class QryopNear extends Qryop {
	private int dist;

	public QryopNear(int dist, Qryop... q) {
		this.dist = dist;
		for (int i = 0; i < q.length; i++) {
			this.args.add(q[i]);
		}
	}

	public QryopNear(int dist) {
		this.dist = dist;
	}

	public void add(Qryop a) {
		this.args.add(a);
	}

	public QryResult evaluate() throws IOException {
		QryResult result = args.get(0).evaluate();

		for (int i = 1; i < args.size(); i++) {
			QryResult iResult = args.get(i).evaluate();

			QryResult crntResult = new QryResult();
			int rDoc = 0, iDoc = 0;
			while (rDoc < result.invertedList.df
					&& iDoc < iResult.invertedList.df) {
				if (result.invertedList.getDocIDByIndex(rDoc) > iResult.invertedList
						.getDocIDByIndex(iDoc)) {
					iDoc++;
				} else if (result.invertedList.getDocIDByIndex(rDoc) < iResult.invertedList
						.getDocIDByIndex(iDoc)) {
					rDoc++;
				} else {
					DocPosting rPost = result.invertedList.postings.get(rDoc);
					DocPosting iPost = iResult.invertedList.postings.get(iDoc);
					DocPosting posting = new DocPosting(
							result.invertedList.getDocIDByIndex(rDoc));
					if (i >= 2) {
						posting.tf = rPost.tf; // this tf represents score!!!
					}
					int rIdx = 0, iIdx = 0;
					while (rIdx < rPost.positions.size() && iIdx < iPost.tf) {
						int rloc = rPost.positions.get(rIdx);
						int iloc = iPost.positions.get(iIdx);
						if (rloc > iloc) {
							iIdx++;
						} else if (iloc - rloc > dist) {
							rIdx++;
						} else {
							posting.add(iloc);
							if (rIdx + 1 < rPost.positions.size()) {
								int new_rloc = rPost.positions.get(rIdx + 1);
								if (new_rloc <= iloc
										&& (iloc - new_rloc <= dist)) {
									rIdx++;
								} else {
									iIdx++;
								}
							} else {
								iIdx++;
							}
						}
					}

					if (posting.positions.size() > 0)
						crntResult.invertedList.add(posting);

					iDoc++;
					rDoc++;
				}
			}

			result = crntResult;
		}
		return result;
	}
}
