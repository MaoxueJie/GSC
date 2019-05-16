package com.gohda.gsc;

import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.schema.SimilarityFactory;

public class GohdaSimilarityFactory extends SimilarityFactory{

	@Override
	public Similarity getSimilarity() {
		return new GohdaSimilarity();
	}

}
