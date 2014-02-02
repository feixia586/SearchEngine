/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
  
  public void insert(int docid, float score, int pos) {
	  scores.add(pos, new ScoreListEntry(docid, score));
  }

  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  public float getDocidScore(int n) {
    return this.scores.get(n).score;
  }
  
  public int containsDocID(int docid) {
	  int size = scores.size();
	  for (int i = 0; i < size; i++){
		  if (scores.get(i).docid == docid)
			  return i;
	  }
	  
	  return -1;
  }
  
  /**
   * set doc score by index
   * @param n the index
   * @param score the value
   */
  public void setScoreByIndex(int n, float score) {
	  this.scores.get(n).score = score;
  }
  
  public void sortScoresByScore() {
	  Collections.sort(scores, new Comparator<ScoreListEntry>() {
		 public int compare(ScoreListEntry s1, ScoreListEntry s2) {
			 if (s1.score != s2.score){
				 if (s2.score > s1.score) return 1;
				 else return -1;
			 } else {
				 try {
				 return QryEval.getExternalDocid(s1.docid).compareTo(QryEval.getExternalDocid(s2.docid));
				 } catch (IOException ex) {
					 System.err.println("Error: I/O error in getExternalDocid.");
					 return s1.docid - s2.docid;
				 }
			 }
		 }
	  });
  }
  
  public void sortScoresByDocID() {
	  Collections.sort(scores, new Comparator<ScoreListEntry>() {
		  public int compare(ScoreListEntry s1, ScoreListEntry s2) {
			  return s1.docid - s2.docid;
		  }
	  });
  }

}
