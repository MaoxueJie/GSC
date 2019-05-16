package org.apache.solr.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.schema.FieldType;
import org.apache.solr.search.QParser;

import com.gohda.gsc.GohdaBooleanQuery;
import com.gohda.gsc.QueryUtils;

public class GohdaBaseQueryParser extends QueryParser{

	public GohdaBaseQueryParser(String defaultField, QParser parser) {
		super(defaultField, parser);
		// TODO Auto-generated constructor stub
	}
	
	public Query rawToNormalExposure(Query q) {
		return rawToNormal(q);
	}
	
	@Override
    Query rawToNormal(Query q) {
        Query normal = q;
        if (q instanceof RawQuery) {
          RawQuery rawq = (RawQuery)q;
          if (rawq.sfield.getType().isTokenized()) {
            normal = rawq.sfield.getType().getFieldQuery(parser, rawq.sfield, rawq.getJoinedExternalVal());
          } else {
            FieldType ft = rawq.sfield.getType();
            if (rawq.getTermCount() == 1) {
              normal = ft.getFieldQuery(this.parser, rawq.sfield, rawq.getExternalVals().get(0));
            } else {
              GohdaBooleanQuery.Builder booleanBuilder = newGohdaBooleanQuery();
              for (String externalVal : rawq.getExternalVals()) {
                Query subq = ft.getFieldQuery(this.parser, rawq.sfield, externalVal);
                booleanBuilder.add(subq, BooleanClause.Occur.SHOULD);
              }
              normal = QueryUtils.build(booleanBuilder, parser);
            }
          }
        }
        return normal;
    }
	protected GohdaBooleanQuery.Builder newGohdaBooleanQuery() {
	    throw new RuntimeException("must implements");
    }
	
	@Override
	protected void addClause(List<BooleanClause> clauses, int conj, int mods, Query q) {
	    boolean required, prohibited;

	    // If this term is introduced by AND, make the preceding term required,
	    // unless it's already prohibited
	    if (clauses.size() > 0 && conj == CONJ_AND) {
	      BooleanClause c = clauses.get(clauses.size()-1);
	      if (!c.isProhibited())
	        clauses.set(clauses.size() - 1, new BooleanClause(c.getQuery(), BooleanClause.Occur.MUST));
	    }

	    if (clauses.size() > 0 && operator == AND_OPERATOR && conj == CONJ_OR) {
	      // If this term is introduced by OR, make the preceding term optional,
	      // unless it's prohibited (that means we leave -a OR b but +a OR b-->a OR b)
	      // notice if the input is a OR b, first term is parsed as required; without
	      // this modification a OR b would parsed as +a OR b
	      BooleanClause c = clauses.get(clauses.size()-1);
	      if (!c.isProhibited())
	        clauses.set(clauses.size() - 1, new BooleanClause(c.getQuery(), BooleanClause.Occur.SHOULD));
	    }

	    // We might have been passed a null query; the term might have been
	    // filtered away by the analyzer.
	    if (q == null)
	      return;

	    if (operator == OR_OPERATOR) {
	      // We set REQUIRED if we're introduced by AND or +; PROHIBITED if
	      // introduced by NOT or -; make sure not to set both.
	      prohibited = (mods == MOD_NOT);
	      required = (mods == MOD_REQ);
	      if (conj == CONJ_AND && !prohibited) {
	        required = true;
	      }
	    } else {
	      // We set PROHIBITED if we're introduced by NOT or -; We set REQUIRED
	      // if not PROHIBITED and not introduced by OR
	      prohibited = (mods == MOD_NOT);
	      required   = (!prohibited && conj != CONJ_OR);
	    }
	    if (required && !prohibited)
	      clauses.add(newBooleanClause(q, BooleanClause.Occur.MUST));
	    else if (!required && !prohibited)
	      clauses.add(newBooleanClause(q, BooleanClause.Occur.SHOULD));
	    else if (!required && prohibited)
	      clauses.add(newBooleanClause(q, BooleanClause.Occur.MUST_NOT));
	    else
	      throw new RuntimeException("Clause cannot be both required and prohibited");
	  }
	protected void add(GohdaBooleanQuery.Builder q, List<Term> current, BooleanClause.Occur operator) {
	    if (current.isEmpty()) {
	      return;
	    }
	    if (current.size() == 1) {
	      q.add(newTermQuery(current.get(0)), operator);
	    } else {
	      q.add(newSynonymQuery(current.toArray(new Term[current.size()])), operator);
	    }
	  }
	protected Query analyzeMultiBoolean(String field, TokenStream stream, BooleanClause.Occur operator) throws IOException {
	    GohdaBooleanQuery.Builder q = newGohdaBooleanQuery();
	    List<Term> currentQuery = new ArrayList<>();
	    
	    TermToBytesRefAttribute termAtt = stream.getAttribute(TermToBytesRefAttribute.class);
	    PositionIncrementAttribute posIncrAtt = stream.getAttribute(PositionIncrementAttribute.class);
	    
	    stream.reset();
	    while (stream.incrementToken()) {
	      if (posIncrAtt.getPositionIncrement() != 0) {
	        add(q, currentQuery, operator);
	        currentQuery.clear();
	      }
	      currentQuery.add(new Term(field, termAtt.getBytesRef()));
	    }
	    add(q, currentQuery, operator);
	    
	    return q.build();
	  }
}
