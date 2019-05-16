/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gohda.gsc;

import static org.apache.solr.parser.SolrQueryParserBase.SynonymQueryStyle.AS_SAME_TERM;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.parser.FastCharStream;
import org.apache.solr.parser.GohdaBaseQueryParser;
import org.apache.solr.parser.ParseException;
import org.apache.solr.parser.QueryParser;
import org.apache.solr.parser.TokenMgrError;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;


/**
 * Solr's default query parser, a schema-driven superset of the classic lucene query parser.
 */
public class GohdaSolrQueryParser extends GohdaBaseQueryParser {

  int flags;
	
  public GohdaSolrQueryParser(QParser parser, String defaultField) {
    super(defaultField, parser);
  }
  public Query parse(String query) throws SyntaxError {
	    ReInit(new FastCharStream(new StringReader(query)));
	    try {
	      // TopLevelQuery is a Query followed by the end-of-input (EOF)
	      Query res = TopLevelQuery(null);  // pass null so we can tell later if an explicit field was provided or not
	      return res!=null ? res : newGohdaBooleanQuery().build();
	    }
	    catch (ParseException pe) {
	      throw new SyntaxError("Cannot parse '" +query+ "': " + pe.getMessage(), pe);
	    }catch(TokenMgrError tme) {
	      throw new SyntaxError("Cannot parse '" +query+ "': " + tme.getMessage(), tme);
	    } catch (BooleanQuery.TooManyClauses tmc) {
	      throw new SyntaxError("Cannot parse '" +query+ "': too many boolean clauses", tmc);
	    }
  }
  
  @Override
  protected GohdaBooleanQuery.Builder newGohdaBooleanQuery() {
	    return new GohdaBooleanQuery.Builder();
  }
  
  public void init(String defaultField, QParser parser) {
	    super.init(defaultField, parser);
	    this.flags = parser.getFlags();
  }

  // Turn on the "filter" bit and return the previous flags for the caller to save
  int startFilter() {
    int oldFlags = flags;
    flags |= QParser.FLAG_FILTER;
    return oldFlags;
  }

  void restoreFlags(int flagsToRestore) {
    flags = flagsToRestore;
  }

  @Override
  protected Query getBooleanQuery(List<BooleanClause> clauses) throws SyntaxError
  {
    if (clauses.size()==0) {
        return null; // all clause words were filtered away by the analyzer.
    }

      SchemaField sfield = null;
      List<RawQuery> fieldValues = null;

      boolean onlyRawQueries = true;
      int allRawQueriesTermCount = 0;
      for (BooleanClause clause : clauses) {
        if (clause.getQuery() instanceof RawQuery) {
          allRawQueriesTermCount += ((RawQuery)clause.getQuery()).getTermCount();
        } else {
          onlyRawQueries = false;
        }
      }
      boolean useTermsQuery = (flags & QParser.FLAG_FILTER)!=0 && allRawQueriesTermCount > TERMS_QUERY_THRESHOLD;

      GohdaBooleanQuery.Builder booleanBuilder = newGohdaBooleanQuery();
      Map<SchemaField, List<RawQuery>> fmap = new HashMap<>();

      for (BooleanClause clause : clauses) {
        Query subq = clause.getQuery();
        if (subq instanceof RawQuery) {
          if (clause.getOccur() != BooleanClause.Occur.SHOULD) {
            // We only collect optional terms for set queries.  Since this isn't optional,
            // convert the raw query to a normal query and handle as usual.
            clause = new BooleanClause( rawToNormalExposure(subq), clause.getOccur() );
          } else {
            // Optional raw query.
            RawQuery rawq = (RawQuery) subq;

            // only look up fmap and type info on a field change
            if (sfield != rawq.sfield) {
              sfield = rawq.sfield;
              fieldValues = fmap.get(sfield);
              // If this field isn't indexed, or if it is indexed and we want to use TermsQuery, then collect this value.
              // We are currently relying on things like PointField not being marked as indexed in order to bypass
              // the "useTermQuery" check.
              if ((fieldValues == null && useTermsQuery) || !sfield.indexed()) {
                fieldValues = new ArrayList<>(2);
                fmap.put(sfield, fieldValues);
              }
            }

            if (fieldValues != null) {
              fieldValues.add(rawq);
              continue;
            }

            clause = new BooleanClause( rawToNormalExposure(subq), clause.getOccur() );
          }
        }

        booleanBuilder.add(clause);
      }


      for (Map.Entry<SchemaField,List<RawQuery>> entry : fmap.entrySet()) {
        sfield = entry.getKey();
        fieldValues = entry.getValue();
        FieldType ft = sfield.getType();

        // TODO: pull more of this logic out to FieldType?  We would need to be able to add clauses to our existing booleanBuilder.
        int termCount = fieldValues.stream().mapToInt(RawQuery::getTermCount).sum();
        if ((sfield.indexed() && termCount < TERMS_QUERY_THRESHOLD) || termCount == 1) {
          // use boolean query instead
          for (RawQuery rawq : fieldValues) {
            Query subq;
            if (ft.isTokenized() && sfield.indexed()) {
              boolean fieldAutoGenPhraseQueries = ft instanceof TextField && ((TextField)ft).getAutoGeneratePhraseQueries();
              boolean fieldEnableGraphQueries = ft instanceof TextField && ((TextField)ft).getEnableGraphQueries();

              SynonymQueryStyle synonymQueryStyle = AS_SAME_TERM;
              if (ft instanceof TextField) {
                synonymQueryStyle = ((TextField)(ft)).getSynonymQueryStyle();
              }

              subq = newFieldQuery(getAnalyzer(), sfield.getName(), rawq.getJoinedExternalVal(),
                  false, fieldAutoGenPhraseQueries, fieldEnableGraphQueries, synonymQueryStyle);
              booleanBuilder.add(subq, BooleanClause.Occur.SHOULD);
            } else {
              for (String externalVal : rawq.getExternalVals()) {
                subq = ft.getFieldQuery(this.parser, sfield, externalVal);
                booleanBuilder.add(subq, BooleanClause.Occur.SHOULD);
              }
            }
          }
        } else {
          List<String> externalVals
              = fieldValues.stream().flatMap(rawq -> rawq.getExternalVals().stream()).collect(Collectors.toList());
          Query subq = ft.getSetQuery(this.parser, sfield, externalVals);
          if (onlyRawQueries && termCount == allRawQueriesTermCount) return subq; // if this is everything, don't wrap in a boolean query
          booleanBuilder.add(subq, BooleanClause.Occur.SHOULD);
        }
      }

      GohdaBooleanQuery bq = QueryUtils.build(booleanBuilder,parser);
      if (bq.clauses().size() == 1) { // Unwrap single SHOULD query
        BooleanClause clause = bq.clauses().iterator().next();
        if (clause.getOccur() == BooleanClause.Occur.SHOULD) {
          return clause.getQuery();
        }
      }
      return bq;
    }

    
 // internal: A simple raw fielded query
    public static class RawQuery extends Query {
      final SchemaField sfield;
      private final List<String> externalVals;

      public RawQuery(SchemaField sfield, String externalVal) {
        this(sfield, Collections.singletonList(externalVal));
      }

      public RawQuery(SchemaField sfield, List<String> externalVals) {
        this.sfield = sfield;
        this.externalVals = externalVals;
      }

      public int getTermCount() {
        return externalVals.size();
      }

      public List<String> getExternalVals() {
        return externalVals;
      }

      public String getJoinedExternalVal() {
        return externalVals.size() == 1 ? externalVals.get(0) : String.join(" ", externalVals);
      }

      @Override
      public String toString(String field) {
        return "RAW(" + field + "," + getJoinedExternalVal() + ")";
      }

      @Override
      public boolean equals(Object obj) {
        return false;
      }

      @Override
      public int hashCode() {
        return 0;
      }
    }
}
