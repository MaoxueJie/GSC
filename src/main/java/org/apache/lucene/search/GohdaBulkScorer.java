package org.apache.lucene.search;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.Bits;

import com.gohda.gsc.GohdaSimilarity;

public class GohdaBulkScorer extends BulkScorer{
	
	private final ArrayList<Weight> weights;
	
	private final BulkScorer original;
	
	public  GohdaBulkScorer(BulkScorer original,ArrayList<Weight> weights) {
		this.original = original;
		this.weights = weights;
	}
	
	@Override
	public int score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
		if (original instanceof Weight.DefaultBulkScorer)
		{
			Field scorerField = null;
			Field docScorerField = null;
			try{
				scorerField = Weight.DefaultBulkScorer.class.getDeclaredField("scorer");
				scorerField.setAccessible(true);
				if (scorerField!=null)
				{
					Scorer scorer = (Scorer)scorerField.get(original);
					if (scorer !=null && scorer instanceof TermScorer)
					{
						
						docScorerField = TermScorer.class.getDeclaredField("docScorer");
						docScorerField.setAccessible(true);
						if (docScorerField!=null)
						{
							Similarity.SimScorer simScorer = (Similarity.SimScorer)docScorerField.get(scorer);
							if (simScorer!=null )
							{
								if (simScorer instanceof GohdaSimilarity.BasicSimScorer)
								{
									((GohdaSimilarity.BasicSimScorer)simScorer).setWeights(weights);
								}
								else if(simScorer instanceof GohdaSimilarity.MultiSimScorer) {
									((GohdaSimilarity.MultiSimScorer)simScorer).setWeights(weights);
								}
							}
						}
					}
				}
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				if (scorerField!=null)
					scorerField.setAccessible(false);
				if (docScorerField!=null)
					docScorerField.setAccessible(false);
			}

			
		}
		return original.score(collector, acceptDocs, min, max);
	}

	@Override
	public long cost() {
		// TODO Auto-generated method stub
		return original.cost();
	}

}
