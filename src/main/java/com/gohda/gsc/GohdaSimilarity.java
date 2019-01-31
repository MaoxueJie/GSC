package com.gohda.gsc;

import java.io.IOException;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;

public class GohdaSimilarity extends Similarity{

	@Override
	public long computeNorm(FieldInvertState state) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String args[])
	{
		System.out.println("\n13.631448 = sum of:\n  10.249881 = weight(query:Ò»ÐÍ in 936) [BM25Similarity], result of:\n    10.249881 = score(doc=936,freq=1.0 = termFreq=1.0\n), product of:\n      10.570428 = idf, computed as log(1 + (docCount - docFreq + 0.5) / (docFreq + 0.5)) from:\n        1.0 = docFreq\n        58447.0 = docCount\n      0.9696751 = tfNorm, computed as (freq * (k1 + 1)) / (freq + k1 * (1 - b + b * fieldLength / avgFieldLength)) from:\n        1.0 = termFreq=1.0\n        1.2 = parameter k1\n        0.75 = parameter b\n        6.502883 = avgFieldLength\n        7.0 = fieldLength\n  3.381567 = weight(query:ÐÍ in 936) [BM25Similarity], result of:\n    3.381567 = score(doc=936,freq=1.0 = termFreq=1.0\n), product of:\n      3.4873195 = idf, computed as log(1 + (docCount - docFreq + 0.5) / (docFreq + 0.5)) from:\n        1787.0 = docFreq\n        58447.0 = docCount\n      0.9696751 = tfNorm, computed as (freq * (k1 + 1)) / (freq + k1 * (1 - b + b * fieldLength / avgFieldLength)) from:\n        1.0 = termFreq=1.0\n        1.2 = parameter k1\n        0.75 = parameter b\n        6.502883 = avgFieldLength\n        7.0 = fieldLength\n");
	}
}
