package com.gohda.gsc;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrQueryParser;
import org.apache.solr.search.SyntaxError;

public class GohdaQParser extends QParser {
	  GohdaSolrQueryParser lparser;

	  public GohdaQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
	    super(qstr, localParams, params, req);
	  }


	  @Override
	  public Query parse() throws SyntaxError {
	    String qstr = getString();
	    if (qstr == null || qstr.length()==0) return null;

	    String defaultField = getParam(CommonParams.DF);
	    lparser = new GohdaSolrQueryParser(this, defaultField);

	    lparser.setDefaultOperator(QueryParsing.parseOP(getParam(QueryParsing.OP)));
	    lparser.setSplitOnWhitespace(StrUtils.parseBool
	      (getParam(QueryParsing.SPLIT_ON_WHITESPACE), SolrQueryParser.DEFAULT_SPLIT_ON_WHITESPACE));
	    lparser.setAllowSubQueryParsing(true);

	    return lparser.parse(qstr);
	  }


	  @Override
	  public String[] getDefaultHighlightFields() {
	    return lparser == null ? new String[]{} : new String[]{lparser.getDefaultField()};
	  }
	  
	 
}
