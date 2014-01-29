/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.List;

public class ScoreList {

  /**
   * A little utilty class to create a <docid, score> object.
   */
  protected class ScoreListEntry {
    private int docid;
    private float score;

    private ScoreListEntry(int docid, float score) {
      this.docid = docid;
      this.score = score;
    }
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   * Append a document score to a score list.
   */
  public void add(int docid, float score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  public float getDocidScore(int n) {
    return this.scores.get(n).score;
  }

}
