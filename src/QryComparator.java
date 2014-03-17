import java.util.Comparator;
import java.util.Map.Entry;

class ScoreComparator implements Comparator<ScoreListEntry> {
	public int compare(ScoreListEntry x, ScoreListEntry y) {
		if (x.score < y.score) {
			return -1;
		}
		if (x.score > y.score) {
			return 1;
		}

		// x.score == y.score
		try {
			return QryEval.getExternalDocid(y.docid).compareTo(
					QryEval.getExternalDocid(x.docid));
		} catch (Exception exp) {
			System.err.println("Error: I/O error in getExternalDocid.");
			return y.docid - x.docid;
		}
	}
}

class WeightComparator implements Comparator<Entry<String, Float>> {
	public int compare(Entry<String, Float> x, Entry<String, Float> y) {
		if (x.getValue() < y.getValue()) {
			return -1;
		}
		if (x.getValue() > y.getValue()) {
			return 1;
		}

		// x.getValue() == y.getValue()
		return y.getKey().compareTo(x.getKey());
	}
}
