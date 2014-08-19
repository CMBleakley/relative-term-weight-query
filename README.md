relative-term-weight-query
==========================

#### What's the problem?

Lucene queries with extremely frequent terms can be expensive. In a disjunctive query with many terms, low frequency terms will dominate the scoring. Omitting high frequency terms should have little impact.

#### Why not use stopwords?

Stopword lists require manual curation and don't take into account the context of a term. For example, "the" is typically considered a stopword but would completely ignore the phrase "The The" 

#### Doesn't the [CommonTermsQuery](http://lucene.apache.org/core/4_6_0/queries/org/apache/lucene/queries/CommonTermsQuery.html) that handles this case?

Yes. Lucene's CommonTermsQuery identifys documents matching the low frequency terms and uses the high frequency terms only when calculating the score. (see: http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-common-terms-query.html) 

#### So why use this?


#### How is the weight calculated?


#### How can I use this?

```
 Query query = new RelativeTermWeightQuery(threshold, mustMatch);
 
 # threshold is the 
 # must match is the
```
