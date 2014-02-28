import java.util.Comparator;


public class ScoreComparator implements Comparator<ScoreListEntry>{
	public int compare(ScoreListEntry x, ScoreListEntry y) {
		if (x.score < y.score) {
			return -1;
		}
		if (x.score > y.score) {
			return 1;
		}
		return 0;
	}
}
