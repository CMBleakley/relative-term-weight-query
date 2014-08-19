package project.lucene;

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.collect.Sets;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RelativeTermWeightQueryTest extends LuceneTestCase {

  public void testBasics() throws IOException {
    Directory dir = newDirectory();

    MockAnalyzer analyzer = new MockAnalyzer(random());
    RandomIndexWriter w = new RandomIndexWriter(random(), dir, analyzer);
    String[] docs = new String[]{
      "3879 E 120th Ave",
      "1415 S 7th Ave",
      "2704 Winding Ridge Ave S",
      "671 Forest Ave",
      "128 Colorado Ave",
      "2609 E McKinley Ave",
      "6771 W 16th Ave",
      "4226 SW 40th Ave",
      "311 Morris Ave",
      "351 Franklin Ave",
      "1513 Cleveland Ave",
      "614 Madison Ave",
      "1919 N Willow Ave",
      "116 Amstel Ave",
      "1739 NW 156th Ave",
      "10303 Arlington Ave",
      "8799 W Colfax Ave",
      "1704 3rd Ave SE",
      "5109 Germantown Ave",
      "1515 E Kansas Ave",
      "2430 Nicollet Ave",
      "200 5th Ave",
      "330 Brookline Ave",
      "1150 NW 72nd Ave",
      "4491 W Keiser Ave",
      "2515 W Sunflower Ave",
      "31950 Little Mack Ave",
      "334 S Patterson Ave",
      "24 Wyckoff Ave",
      "1081 Ave of the Stars"
    };

    for (int i = 0; i < docs.length; i++) {
      Document doc = new Document();
      doc.add(newStringField("id", "" + i, Field.Store.YES));
      doc.add(newTextField("field", docs[i], Field.Store.YES));
      w.addDocument(doc);
    }

    IndexReader r = w.getReader();
    IndexSearcher s = newSearcher(r);

    RelativeTermWeightQuery query = new RelativeTermWeightQuery(1.0f);
    query.add(new Term("field", "ave"));
    query.add(new Term("field", "of"));
    query.add(new Term("field", "the"));
    query.add(new Term("field", "stars"));

    Query q = query.rewrite(r);
    HashSet<Term> terms = new HashSet<Term>();
    q.extractTerms(terms);

    /* there should be all 4 terms at max threshold */
    assertTrue(terms.size() == 4);

    r.close();
    w.close();
    dir.close();
  }
}
