import java.util.*;
import java.util.Map.Entry;

public class QryExpander {
	private Map<String, Integer> _df;
	private Map<String, Integer> _ctf;
	private int _termLength;
	private static final String[] add_stopWordsVal = new String[] { "edit",
			"from", "have", "has", "he", "here", "his", "http", "its", "page",
			"she", "text", "use", "we", "were", "which", "wikipedia" };
	private static final Set<String> add_StopWords = new HashSet<String>(
			Arrays.asList(add_stopWordsVal));

	/**
	 * init the df and ctf according to the relevant documents
	 * 
	 * @param entries
	 *            the top entries used as pseudo revelance feedback
	 * @throws Exception
	 */
	public void initStat(List<ScoreListEntry> entries) throws Exception {
		int size = entries.size();

		_df = new HashMap<String, Integer>();
		_ctf = new HashMap<String, Integer>();
		_termLength = 0;
		for (int i = 0; i < size; i++) {
			TermVector tv = new TermVector(entries.get(i).docid, "body");
			int len = tv.positionsLength();

			HashSet<String> dfHelper = new HashSet<String>();
			for (int j = 0; j < len; j++) {
				String stem = tv.stemString(tv.stemAt(j));
				if (stem == null)
					continue;

				// the ctf
				if (_ctf.get(stem) == null) {
					_ctf.put(stem, 1);
				} else {
					_ctf.put(stem, _ctf.get(stem) + 1);
				}
				dfHelper.add(stem);

				// the termLength
				_termLength++;
			}

			// the df
			for (String term : dfHelper) {
				if (_df.get(term) == null) {
					_df.put(term, 1);
				} else {
					_df.put(term, _df.get(term) + 1);
				}
			}

		}
	}

	public String expandQuery(QryResult result) throws Exception {
		List<ScoreListEntry> topEntries = getTopEntries(result,
				QryParams.QE_fbDocs);

		// statistics must be initiated before actually doing expansion
		initStat(topEntries);

		Map<String, Float> termWeight = calcTermWeight(topEntries);
		List<Entry<String, Float>> topWeightedTerms = getTopWeightedTerms(
				termWeight, QryParams.QE_fbTerms);

		StringBuilder Ex_qry = new StringBuilder("#WEIGHT(");
		for (int i = 0; i < topWeightedTerms.size(); i++) {
			Entry<String, Float> entry = topWeightedTerms.get(i);
			Ex_qry.append(entry.getValue() + " ");
			Ex_qry.append(entry.getKey() + " ");
		}
		Ex_qry.append(")");

		return Ex_qry.toString();
	}

	public String combineQuery(String original, String expended) {
		StringBuilder combinedQry = new StringBuilder("#WEIGHT(");

		combinedQry.append(QryParams.QE_fbOrigWeight + " #AND(");
		combinedQry.append(original + ") ");

		combinedQry.append((1 - QryParams.QE_fbOrigWeight) + " " + expended);
		combinedQry.append(")");

		return combinedQry.toString();
	}

	public Map<String, Float> calcTermWeight(List<ScoreListEntry> entries)
			throws Exception {
		int size = entries.size();

		Map<String, Float> termWeights = new HashMap<String, Float>();
		for (int i = 0; i < size; i++) {
			TermVector tv = new TermVector(entries.get(i).docid, "body");
			float docScore = entries.get(i).score;
			if (docScore <= 0) {
				docScore = (float)Math.exp(docScore);
			}

			HashSet<Integer> uniqueTermIdx = new HashSet<Integer>();
			int len = tv.positionsLength();
			// find out the unique term index
			for (int j = 0; j < len; j++) {
				String stem = tv.stemString(tv.stemAt(j));
				if (stem == null)
					continue;

				uniqueTermIdx.add(tv.stemAt(j));
			}

			// calculate the term weight
			for (Integer idx : uniqueTermIdx) {
				int tf = tv.stemFreq(idx);
				String term = tv.stemString(idx);
				float smo = calcSmoScore(term, size);
				float score = (tf + QryParams.QE_fbMu * smo)
						/ (float) (tv.positionsLength() + QryParams.QE_fbMu)
						* docScore;
				if (termWeights.get(term) == null) {
					termWeights.put(term, score);
				} else {
					termWeights.put(term, termWeights.get(term) + score);
				}
			}
		}

		return termWeights;

	}

	private float calcSmoScore(String stem, int size) {
		float smo = 0;

		if (QryParams.Indri_smo == SmoothTech.DF) {
			int df = _df.get(stem);
			int len_docs = size;
			smo = df / (float) len_docs;
		} else if (QryParams.Indri_smo == SmoothTech.CTF) {
			int ctf = _ctf.get(stem);
			long len_terms = _termLength;
			smo = ctf / (float) len_terms;
		} else {
			System.err.println("Error: QryExpander Indri_smo invalid!");
			System.exit(1);
		}

		return smo;
	}

	/**
	 * Get the top n ScoreListEntry in result
	 * 
	 * @param result
	 *            the result of retrieval
	 * @param n
	 * @return at most n ScoreListEntry with highest scores
	 */
	public List<ScoreListEntry> getTopEntries(QryResult result, int n) {
		assert (n > 0);

		List<ScoreListEntry> entries = new ArrayList<ScoreListEntry>();
		int size = Math.min(n, result.docScores.scores.size());

		if (size <= 0)
			return entries;

		Comparator<ScoreListEntry> comp = new ScoreComparator();
		PriorityQueue<ScoreListEntry> que = new PriorityQueue<ScoreListEntry>(
				size, comp);

		List<ScoreListEntry> lt = result.docScores.scores;
		for (int i = 0; i < size; i++) {
			que.add(lt.get(i));
		}

		int total_item = lt.size();
		for (int i = size; i < total_item; i++) {
			if (comp.compare(lt.get(i), que.peek()) > 0) {
				que.poll();
				que.add(lt.get(i));
			}
		}

		// use a stack to reverse the order
		Stack<ScoreListEntry> stk = new Stack<ScoreListEntry>();
		while (que.size() != 0) {
			stk.push(que.poll());
		}
		while (stk.size() != 0) {
			entries.add(stk.pop());
		}

		return entries;
	}

	public List<Entry<String, Float>> getTopWeightedTerms(
			Map<String, Float> termWeights, int n) {
		List<Entry<String, Float>> topWeightedTerms = new ArrayList<Entry<String, Float>>();

		int size = Math.min(n, termWeights.size());
		if (size <= 0)
			return topWeightedTerms;

		Comparator<Entry<String, Float>> comp = new WeightComparator();
		PriorityQueue<Entry<String, Float>> que = new PriorityQueue<Map.Entry<String, Float>>(
				size, comp);

		Iterator<Entry<String, Float>> it = termWeights.entrySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			Entry<String, Float> entry = it.next();
			if (isTermValid(entry)) {
				que.add(entry);
				if (++i >= size)
					break;
			}
		}

		while (it.hasNext()) {
			Entry<String, Float> entry = it.next();
			if (comp.compare(entry, que.peek()) > 0 && isTermValid(entry)) {
				que.poll();
				que.add(entry);
			}
		}

		// use a stack to reverse the order
		Stack<Entry<String, Float>> stk = new Stack<Map.Entry<String, Float>>();
		while (que.size() != 0) {
			stk.push(que.poll());
		}
		while (stk.size() != 0) {
			topWeightedTerms.add(stk.pop());
		}

		normalizeWeight(topWeightedTerms);

		return topWeightedTerms;
	}

	/**
	 * check whether the term entry is valid
	 * 
	 * @param entry
	 *            the term entry
	 * @return true if the term entry is valid; false it isn't
	 */
	private boolean isTermValid(Entry<String, Float> entry) {
		String term = entry.getKey();

		// discard the numerical term
		try {
			Double.parseDouble(term);
			return false;
		} catch (NumberFormatException nfex) {

		}

		// discard the term that contains ".", ","
		if (term.contains(".") || term.contains(","))
			return false;
		
		// discard additional stop words
		if (add_StopWords.contains(term))
			return false;

		return true;
	}

	/**
	 * Normalize the term weight entries
	 * 
	 * @param weightEntries
	 *            the weight entries to be normalized
	 */
	private void normalizeWeight(List<Entry<String, Float>> weightEntries) {
		float sum = 0;
		for (Entry<String, Float> entry : weightEntries) {
			sum += entry.getValue();
		}

		for (int i = 0; i < weightEntries.size(); i++) {
			Entry<String, Float> entry = weightEntries.get(i);
			entry.setValue(entry.getValue() / sum);
			weightEntries.set(i, entry);
		}
	}

}
