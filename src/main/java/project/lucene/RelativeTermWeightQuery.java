package project.lucene;

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

import org.apache.lucene.index.*;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.ToStringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class RelativeTermWeightQuery extends Query {
  protected final List<Term> terms = new ArrayList<Term>();
  protected final float threshold;
  protected final int mm;

  public RelativeTermWeightQuery(float threshold, int mm) {
    if (threshold <= 0.0f || threshold > 1.0f) {
      throw new IllegalArgumentException("threshold must be between (0..1]");
    }
    this.threshold = threshold;
    this.mm = mm;
  }

  public RelativeTermWeightQuery(float threshold) {
    this(threshold, 0);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (this.terms.isEmpty()) {
      return new BooleanQuery();
    } else if (this.terms.size() == 1) {
      final Query tq = newTermQuery(this.terms.get(0), null);
      tq.setBoost(getBoost());
      return tq;
    }
    final List<AtomicReaderContext> leaves = reader.leaves();
    final int totalDocs = reader.numDocs();
    final TermContext[] contextArray = new TermContext[terms.size()];
    final Term[] queryTerms = this.terms.toArray(new Term[0]);
    final Double totalIdf = 0.0;

    collectTermContext(reader, leaves, contextArray, queryTerms);
    return buildQuery(totalDocs, contextArray, queryTerms);
  }

  private class TermWeight implements Comparable<TermWeight> {
    private double idf;
    private Term queryTerm;

    public TermWeight(int totalDocs, int docFreq, Term queryTerm) {
      this.queryTerm = queryTerm;
      this.idf = Math.log(totalDocs * 1.0 / docFreq);
    }

    public double getIdf() {
      return this.idf;
    }

    public Term getQueryTerm() {
      return this.queryTerm;
    }

    @Override
    public int compareTo(TermWeight o) {
      if (o == null) {
        return -1;
      }
      if (o.getIdf() > idf) {
        return 1;
      } else if (o.getIdf() == idf) {
        return 0;
      } else {
        return -1;
      }
    }
  }

  protected Query buildQuery(final int totalDocs, final TermContext[] contextArray, final Term[] queryTerms) {
    BooleanQuery query = new BooleanQuery(true);
    Set<TermWeight> weights = new TreeSet<TermWeight>();

    double totalIdf = 0.0;
    for (int i = 0; i < queryTerms.length; i++) {
      if (contextArray[i] != null) {
        TermWeight weight = new TermWeight(totalDocs, contextArray[i].docFreq(), queryTerms[i]);
        totalIdf += weight.getIdf();
        weights.add(weight);
      }
    }

    double runningWeight = 0.0;
    for (TermWeight weight : weights) {
      if (runningWeight >= threshold) {
        break;
      }
      query.add(new TermQuery(weight.getQueryTerm()), BooleanClause.Occur.SHOULD);
      runningWeight += weight.getIdf() / totalIdf;
    }

    query.setBoost(getBoost());
    return query;
  }


  /* Because an index is composed of many leaves and because a term can be
     contained within many leaves, we must visit each leaf to collect all
     term information.
   */

  public void collectTermContext(IndexReader reader, List<AtomicReaderContext> leaves, TermContext[] contextArray,Term[] queryTerms) throws IOException {
    TermsEnum termsEnum = null;
    for (AtomicReaderContext context : leaves) {
      final Fields fields = context.reader().fields();
      if (fields == null) {
        // reader has no fields
        continue;
      }
      for (int i = 0; i < queryTerms.length; i++) {
        Term term = queryTerms[i];
        TermContext termContext = contextArray[i];
        final Terms terms = fields.terms(term.field());
        if (terms == null) {
          // field does not exist
          continue;
        }
        termsEnum = terms.iterator(termsEnum);
        assert termsEnum != null;

        if (termsEnum == TermsEnum.EMPTY) continue;
        if (termsEnum.seekExact(term.bytes())) {
          if (termContext == null) {
            contextArray[i] = new TermContext(reader.getContext(),
                termsEnum.termState(), context.ord, termsEnum.docFreq(),
                termsEnum.totalTermFreq());
          } else {
            termContext.register(termsEnum.termState(), context.ord,
                termsEnum.docFreq(), termsEnum.totalTermFreq());
          }
        }
      }
    }
  }

  /** Prints a user-readable version of this query. */

  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    boolean needParens = (getBoost() != 1.0);
    if (needParens) {
      buffer.append("(");
    }
    for (int i = 0; i < terms.size(); i++) {
      Term t = terms.get(i);
      buffer.append(newTermQuery(t, null).toString());
      if (i != terms.size() - 1) buffer.append(", ");
    }
    if (needParens) {
      buffer.append(")");
    }

    buffer.append('~');
    buffer.append("(");
    buffer.append(threshold);
    buffer.append(")");

    if (getBoost() != 1.0f) {
      buffer.append(ToStringUtils.boost(getBoost()));
    }
    return buffer.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Float.floatToIntBits(threshold);
    result = prime * result + ((terms == null) ? 0 : terms.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    RelativeTermWeightQuery other = (RelativeTermWeightQuery) obj;
    if (threshold != other.threshold) return false;
    if (terms == null) {
      if (other.terms != null) return false;
    } else if (!terms.equals(other.terms)) return false;
    return true;
  }

  protected Query newTermQuery(Term term, TermContext context) {
    return context == null ? new TermQuery(term) : new TermQuery(term, context);
  }
}