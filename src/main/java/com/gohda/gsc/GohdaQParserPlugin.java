package com.gohda.gsc;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

public class GohdaQParserPlugin extends QParserPlugin{

	@Override
	public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		// TODO Auto-generated method stub
		return new GohdaQParser(qstr,localParams,params,req);
	}

}
