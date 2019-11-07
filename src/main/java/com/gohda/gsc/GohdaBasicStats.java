package com.gohda.gsc;

import org.apache.lucene.index.Terms;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;

public class GohdaBasicStats extends Similarity.SimWeight {
	  final String field;
	  /** The number of documents. */
	  protected long numberOfDocuments;
	  /** The total number of tokens in the field. */
	  protected long numberOfFieldTokens;
	  /** The average field length. */
	  protected float avgFieldLength;
	  /** The document frequency. */
	  protected long docFreq;
	  /** The total number of occurrences of this term across all documents. */
	  protected long totalTermFreq;
	  
	  final String value;
	  
	  protected GohdaBooleanQuery query;
	  
	  // -------------------------- Boost-related stuff --------------------------

	  /** A query boost. Should be applied as a multiplicative factor to the score. */
	  protected final float boost;
	  
	  /** Constructor. */
	  public GohdaBasicStats(String field, float boost,String value) {
	    this.field = field;
	    this.boost = boost;
	    this.value = value;
	  }
	  
	  // ------------------------- Getter/setter methods -------------------------
	  
	  /** Returns the number of documents. */
	  public long getNumberOfDocuments() {
	    return numberOfDocuments;
	  }
	  
	  /** Sets the number of documents. */
	  public void setNumberOfDocuments(long numberOfDocuments) {
	    this.numberOfDocuments = numberOfDocuments;
	  }
	  
	  /**
	   * Returns the total number of tokens in the field.
	   * @see Terms#getSumTotalTermFreq()
	   */
	  public long getNumberOfFieldTokens() {
	    return numberOfFieldTokens;
	  }
	  
	  /**
	   * Sets the total number of tokens in the field.
	   * @see Terms#getSumTotalTermFreq()
	   */
	  public void setNumberOfFieldTokens(long numberOfFieldTokens) {
	    this.numberOfFieldTokens = numberOfFieldTokens;
	  }
	  
	  /** Returns the average field length. */
	  public float getAvgFieldLength() {
	    return avgFieldLength;
	  }
	  
	  /** Sets the average field length. */
	  public void setAvgFieldLength(float avgFieldLength) {
	    this.avgFieldLength = avgFieldLength;
	  }
	  
	  /** Returns the document frequency. */
	  public long getDocFreq() {
	    return docFreq;
	  }
	  
	  /** Sets the document frequency. */
	  public void setDocFreq(long docFreq) {
	    this.docFreq = docFreq;
	  }
	  
	  /** Returns the total number of occurrences of this term across all documents. */
	  public long getTotalTermFreq() {
	    return totalTermFreq;
	  }
	  
	  /** Sets the total number of occurrences of this term across all documents. */
	  public void setTotalTermFreq(long totalTermFreq) {
	    this.totalTermFreq = totalTermFreq;
	  }
	  
	  /** Returns the total boost. */
	  public float getBoost() {
	    return boost;
	  }

	  public String getValue() {
		return value;
	  }
	
	  public GohdaBooleanQuery getQuery() {
		return query;
	  }

	  public void setQuery(GohdaBooleanQuery query) {
		this.query = query;
	  }

	  public String getField() {
		return field;
	  }
	  
	}
