package com.gohda.gsc;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GohdaSimilarity extends Similarity{
	
	public static void main(String args[])
	{
		System.out.println("\n13.458811 = sum of:\n  5.170505 = weight(query:糖尿病 in 37996) [BM25Similarity], result of:\n    5.170505 = score(doc=37996,freq=1.0 = termFreq=1.0\n), product of:\n      4.03036 = idf, computed as log(1 + (docCount - docFreq + 0.5) / (docFreq + 0.5)) from:\n        1038.0 = docFreq\n        58447.0 = docCount\n      1.282889 = tfNorm, computed as (freq * (k1 + 1)) / (freq + k1 * (1 - b + b * fieldLength / avgFieldLength)) from:\n        1.0 = termFreq=1.0\n        1.2 = parameter k1\n        0.75 = parameter b\n        6.507913 = avgFieldLength\n        3.0 = fieldLength\n  5.163114 = weight(query:糖尿 in 37996) [BM25Similarity], result of:\n    5.163114 = score(doc=37996,freq=1.0 = termFreq=1.0\n), product of:\n      4.024599 = idf, computed as log(1 + (docCount - docFreq + 0.5) / (docFreq + 0.5)) from:\n        1044.0 = docFreq\n        58447.0 = docCount\n      1.282889 = tfNorm, computed as (freq * (k1 + 1)) / (freq + k1 * (1 - b + b * fieldLength / avgFieldLength)) from:\n        1.0 = termFreq=1.0\n        1.2 = parameter k1\n        0.75 = parameter b\n        6.507913 = avgFieldLength\n        3.0 = fieldLength\n  3.1251917 = weight(query:病 in 37996) [BM25Similarity], result of:\n    3.1251917 = score(doc=37996,freq=1.0 = termFreq=1.0\n), product of:\n      2.4360578 = idf, computed as log(1 + (docCount - docFreq + 0.5) / (docFreq + 0.5)) from:\n        5114.0 = docFreq\n        58447.0 = docCount\n      1.282889 = tfNorm, computed as (freq * (k1 + 1)) / (freq + k1 * (1 - b + b * fieldLength / avgFieldLength)) from:\n        1.0 = termFreq=1.0\n        1.2 = parameter k1\n        0.75 = parameter b\n        6.507913 = avgFieldLength\n        3.0 = fieldLength\n");
	}
	
	private static Log log = LogFactory.getLog(GohdaSimilarity.class);

	 /** For {@link #log2(double)}. Precomputed for efficiency reasons. */
	  private static final double LOG_2 = Math.log(2);
	  
	  /** 
	   * True if overlap tokens (tokens with a position of increment of zero) are
	   * discounted from the document's length.
	   */
	  protected boolean discountOverlaps = true;
	  
	  /**
	   * Sole constructor. (For invocation by subclass 
	   * constructors, typically implicit.)
	   */
	  public GohdaSimilarity() {}
	  
	  /** Determines whether overlap tokens (Tokens with
	   *  0 position increment) are ignored when computing
	   *  norm.  By default this is true, meaning overlap
	   *  tokens do not count when computing norms.
	   *
	   *  @lucene.experimental
	   *
	   *  @see #computeNorm
	   */
	  public void setDiscountOverlaps(boolean v) {
	    discountOverlaps = v;
	  }

	  /**
	   * Returns true if overlap tokens are discounted from the document's length. 
	   * @see #setDiscountOverlaps 
	   */
	  public boolean getDiscountOverlaps() {
	    return discountOverlaps;
	  }
	  
	  @Override
	  public final SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
		  
		log.info("computeWeight------------------------------termStats="+JSONArray.fromObject(termStats)+" collectionStats="+JSONObject.fromObject(collectionStats) + " boost="+boost);  
		  
	    BasicStats stats[] = new BasicStats[termStats.length];
	    for (int i = 0; i < termStats.length; i++) {
	      stats[i] = newStats(collectionStats.field(), boost);
	      fillBasicStats(stats[i], collectionStats, termStats[i]);
	    }
	    return stats.length == 1 ? stats[0] : new MultiStats(stats);
	  }
	  
	  /** Factory method to return a custom stats object */
	  protected BasicStats newStats(String field, float boost) {
	    return new BasicStats(field, boost);
	  }
	  
	  /** Fills all member fields defined in {@code BasicStats} in {@code stats}. 
	   *  Subclasses can override this method to fill additional stats. */
	  protected void fillBasicStats(BasicStats stats, CollectionStatistics collectionStats, TermStatistics termStats) {
	    // #positions(field) must be >= #positions(term)
	    assert collectionStats.sumTotalTermFreq() == -1 || collectionStats.sumTotalTermFreq() >= termStats.totalTermFreq();
	    long numberOfDocuments = collectionStats.docCount() == -1 ? collectionStats.maxDoc() : collectionStats.docCount();
	    
	    long docFreq = termStats.docFreq();
	    long totalTermFreq = termStats.totalTermFreq();

	    // frequencies are omitted, all postings have tf=1, so totalTermFreq = docFreq
	    if (totalTermFreq == -1) {
	      totalTermFreq = docFreq;
	    }

	    final long numberOfFieldTokens;
	    final float avgFieldLength;

	    if (collectionStats.sumTotalTermFreq() == -1) {
	      // frequencies are omitted, so sumTotalTermFreq = # postings
	      if (collectionStats.sumDocFreq() == -1) {
	        // theoretical case only: remove!
	        numberOfFieldTokens = docFreq;
	        avgFieldLength = 1f;
	      } else {
	        numberOfFieldTokens = collectionStats.sumDocFreq();
	        avgFieldLength = (float) (collectionStats.sumDocFreq() / (double)numberOfDocuments);
	      }
	    } else {
	      numberOfFieldTokens = collectionStats.sumTotalTermFreq();
	      avgFieldLength = (float) (collectionStats.sumTotalTermFreq() / (double)numberOfDocuments);
	    }
	 
	    // TODO: add sumDocFreq for field (numberOfFieldPostings)
	    stats.setNumberOfDocuments(numberOfDocuments);
	    stats.setNumberOfFieldTokens(numberOfFieldTokens);
	    stats.setAvgFieldLength(avgFieldLength);
	    stats.setDocFreq(docFreq);
	    stats.setTotalTermFreq(totalTermFreq);
	  }
	  
	  /**
	   * Scores the document {@code doc}.
	   * <p>Subclasses must apply their scoring formula in this class.</p>
	   * @param stats the corpus level statistics.
	   * @param freq the term frequency.
	   * @param docLen the document length.
	   * @return the score.
	   */
	  protected float score(BasicStats stats, float freq, float docLen,String f) {
		  log.info("score------------------------------stats="+JSONObject.fromObject(stats)+" freq="+freq + " docLen="+docLen + " f="+f);
		  return 1f;
	  }
	  
	  /**
	   * Subclasses should implement this method to explain the score. {@code expl}
	   * already contains the score, the name of the class and the doc id, as well
	   * as the term frequency and its explanation; subclasses can add additional
	   * clauses to explain details of their scoring formulae.
	   * <p>The default implementation does nothing.</p>
	   * 
	   * @param subExpls the list of details of the explanation to extend
	   * @param stats the corpus level statistics.
	   * @param doc the document id.
	   * @param freq the term frequency.
	   * @param docLen the document length.
	   */
	  protected void explain(
	      List<Explanation> subExpls, BasicStats stats, int doc, float freq, float docLen) {}
	  
	  /**
	   * Explains the score. The implementation here provides a basic explanation
	   * in the format <em>score(name-of-similarity, doc=doc-id,
	   * freq=term-frequency), computed from:</em>, and
	   * attaches the score (computed via the {@link #score(BasicStats, float, float)}
	   * method) and the explanation for the term frequency. Subclasses content with
	   * this format may add additional details in
	   * {@link #explain(List, BasicStats, int, float, float)}.
	   *  
	   * @param stats the corpus level statistics.
	   * @param doc the document id.
	   * @param freq the term frequency and its explanation.
	   * @param docLen the document length.
	   * @return the explanation.
	   */
	  protected Explanation explain(
	      BasicStats stats, int doc, Explanation freq, float docLen,String f) {
	    List<Explanation> subs = new ArrayList<Explanation>();
	    explain(subs, stats, doc, freq.getValue(), docLen);
	    
	    return Explanation.match(
	        score(stats, freq.getValue(), docLen,f),
	        "score(" + getClass().getSimpleName() + ", doc=" + doc + ", freq=" + freq.getValue() +"), computed from:",
	        subs);
	  }
	  
	  @Override
	  public final SimScorer simScorer(SimWeight stats, LeafReaderContext context) throws IOException {
	    int indexCreatedVersionMajor = context.reader().getMetaData().getCreatedVersionMajor();
	    try {
		    if (stats instanceof MultiStats) {
		      // a multi term query (e.g. phrase). return the summation, 
		      // scoring almost as if it were boolean query
		      SimWeight subStats[] = ((MultiStats) stats).subStats;
		      SimScorer subScorers[] = new SimScorer[subStats.length];
		      for (int i = 0; i < subScorers.length; i++) {
		        BasicStats basicstats = (BasicStats) subStats[i];
		        Field f = BasicStats.class.getDeclaredField("field");
			    f.setAccessible(true);
		        subScorers[i] = new BasicSimScorer(basicstats, indexCreatedVersionMajor, context.reader().getNormValues((String)f.get(basicstats)),context.reader().getSortedDocValues((String)f.get(basicstats)+"_str"));
		      }
		      return new MultiSimScorer(subScorers);
		    } else {
		      BasicStats basicstats = (BasicStats) stats;
		      
		      Field f = BasicStats.class.getDeclaredField("field");
		      f.setAccessible(true);
		      log.info("Field------------------------------field="+f.get(basicstats) + " normValues="+JSONObject.fromObject(context.reader().getNormValues((String)f.get(basicstats))) );
		      return new BasicSimScorer(basicstats, indexCreatedVersionMajor, context.reader().getNormValues((String)f.get(basicstats)),context.reader().getSortedDocValues((String)f.get(basicstats)+"_str"));
		    }
	    }catch(Exception e)
	    {
	    	e.printStackTrace();
	    }
	    return null;
	  }
	  
	  /**
	   * Subclasses must override this method to return the name of the Similarity
	   * and preferably the values of parameters (if any) as well.
	   */
	  @Override
	  public  String toString() {
		  return "";
	  }

	  // ------------------------------ Norm handling ------------------------------
	  
	  /** Cache of decoded bytes. */
	  private static final float[] OLD_LENGTH_TABLE = new float[256];
	  private static final float[] LENGTH_TABLE = new float[256];

	  static {
	    for (int i = 1; i < 256; i++) {
	      float f = SmallFloat.byte315ToFloat((byte)i);
	      OLD_LENGTH_TABLE[i] = 1.0f / (f*f);
	    }
	    OLD_LENGTH_TABLE[0] = 1.0f / OLD_LENGTH_TABLE[255]; // otherwise inf

	    for (int i = 0; i < 256; i++) {
	      LENGTH_TABLE[i] = SmallFloat.byte4ToInt((byte) i);
	    }
	  }

	  /** Encodes the document length in the same way as {@link BM25Similarity}. */
	  @Override
	  public final long computeNorm(FieldInvertState state) {
		  log.info("computeNorm------------------------------state="+JSONObject.fromObject(state));
	    final int numTerms;
	    if (discountOverlaps)
	      numTerms = state.getLength() - state.getNumOverlap();
	    else
	      numTerms = state.getLength();
	    int indexCreatedVersionMajor = state.getIndexCreatedVersionMajor();
	    if (indexCreatedVersionMajor >= 7) {
	      return SmallFloat.intToByte4(numTerms);
	    } else {
	      return SmallFloat.floatToByte315((float) (1 / Math.sqrt(numTerms)));
	    }
	  }

	  // ----------------------------- Static methods ------------------------------
	  
	  /** Returns the base two logarithm of {@code x}. */
	  public static double log2(double x) {
	    // Put this to a 'util' class if we need more of these.
	    return Math.log(x) / LOG_2;
	  }
	  
	  // --------------------------------- Classes ---------------------------------
	  
	  /** Delegates the {@link #score(int, float)} and
	   * {@link #explain(int, Explanation)} methods to
	   * {@link SimilarityBase#score(BasicStats, float, float)} and
	   * {@link SimilarityBase#explain(BasicStats, int, Explanation, float)},
	   * respectively.
	   */
	  final class BasicSimScorer extends SimScorer {
	    private final BasicStats stats;
	    private final NumericDocValues norms;
	    private final SortedDocValues docValues;
	    private final float[] normCache;
	    
	    BasicSimScorer(BasicStats stats, int indexCreatedVersionMajor, NumericDocValues norms,SortedDocValues docValues) throws IOException {
	      this.stats = stats;
	      this.norms = norms;
	      this.docValues = docValues;
	      this.normCache = indexCreatedVersionMajor >= 7 ? LENGTH_TABLE : OLD_LENGTH_TABLE;
	    }

	    float getLengthValue(int doc) throws IOException {
	      if (norms == null) {
	        return 1F;
	      }
	      if (norms.advanceExact(doc)) {
	        return normCache[Byte.toUnsignedInt((byte) norms.longValue())];
	      } else {
	        return 0;
	      }
	    }
	    String getQueryValue(int doc) throws IOException {
		  if (docValues == null) {
	        return null;
	      }
	      if (docValues.advanceExact(doc)) {
	        return docValues.binaryValue().utf8ToString();
	      } else {
	        return "";
	      }
	    }
	    
	    @Override
	    public float score(int doc, float freq) throws IOException {
	      // We have to supply something in case norms are omitted
	      return GohdaSimilarity.this.score(stats, freq, getLengthValue(doc),getQueryValue(doc));
	    }

	    @Override
	    public Explanation explain(int doc, Explanation freq) throws IOException {
	      return GohdaSimilarity.this.explain(stats, doc, freq, getLengthValue(doc),getQueryValue(doc));
	    }

	    @Override
	    public float computeSlopFactor(int distance) {
	      return 1.0f / (distance + 1);
	    }

	    @Override
	    public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
	      return 1f;
	    }
	  }
	  
	  static class MultiSimScorer extends SimScorer {
		    private final SimScorer subScorers[];
		    
		    MultiSimScorer(SimScorer subScorers[]) {
		      this.subScorers = subScorers;
		    }
		    
		    @Override
		    public float score(int doc, float freq) throws IOException {
		      float sum = 0.0f;
		      for (SimScorer subScorer : subScorers) {
		        sum += subScorer.score(doc, freq);
		      }
		      return sum;
		    }

		    @Override
		    public Explanation explain(int doc, Explanation freq) throws IOException {
		      List<Explanation> subs = new ArrayList<Explanation>();
		      for (SimScorer subScorer : subScorers) {
		        subs.add(subScorer.explain(doc, freq));
		      }
		      return Explanation.match(score(doc, freq.getValue()), "sum of:", subs);
		    }

		    @Override
		    public float computeSlopFactor(int distance) {
		      return subScorers[0].computeSlopFactor(distance);
		    }

		    @Override
		    public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
		      return subScorers[0].computePayloadFactor(doc, start, end, payload);
		    }
		  }

		  static class MultiStats extends SimWeight {
		    final SimWeight subStats[];
		    
		    MultiStats(SimWeight subStats[]) {
		      this.subStats = subStats;
		    }
		  }
	
}