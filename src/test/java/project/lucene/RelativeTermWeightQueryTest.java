package project.lucene;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;

import java.io.IOException;
import java.util.HashSet;

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
    RelativeTermWeightQuery query;
    HashSet<Term> terms;

    /* make sure all terms are included at max threshold */

    query = new RelativeTermWeightQuery(1.0f);
    query.add(new Term("field", "ave"));
    query.add(new Term("field", "of"));
    query.add(new Term("field", "the"));
    query.add(new Term("field", "stars"));

    terms = new HashSet<Term>();
    query.rewrite(r).extractTerms(terms);
    assertTrue(terms.size() == 4);

    /* ensure terms that are not included in the index are not included in the query */

    query = new RelativeTermWeightQuery(1.0f);
    query.add(new Term("field", "ave"));
    query.add(new Term("field", "of"));
    query.add(new Term("field", "the"));
    query.add(new Term("field", "donkeys"));  // not in the index

    terms = new HashSet<Term>();
    query.rewrite(r).extractTerms(terms);
    assertTrue(terms.size() == 3);

    /* at min threshold make sure atleast 1 term included */

    query = new RelativeTermWeightQuery(0.00001f);
    query.add(new Term("field", "ave"));
    query.add(new Term("field", "of"));
    query.add(new Term("field", "the"));
    query.add(new Term("field", "donkeys"));  // not in the index

    terms = new HashSet<Term>();
    query.rewrite(r).extractTerms(terms);
    assertTrue(terms.size() == 1);

    /* at min threshold ensure minimum number of terms included */

    query = new RelativeTermWeightQuery(0.000001f, 3);
    query.add(new Term("field", "ave"));
    query.add(new Term("field", "of"));
    query.add(new Term("field", "the"));
    query.add(new Term("field", "donkeys"));  // not in the index

    terms = new HashSet<Term>();
    query.rewrite(r).extractTerms(terms);
    assertTrue("min threshold", terms.size() == 3);

    /* set threshold such that commons term "ave" is omitted */

    query = new RelativeTermWeightQuery(0.7f);
    query.add(new Term("field", "ave"));
    query.add(new Term("field", "of"));
    query.add(new Term("field", "the"));
    query.add(new Term("field", "stars"));  // not in the index

    terms = new HashSet<Term>();
    assertTrue("min threshold", terms.size() == 3);

    r.close();
    w.close();
    dir.close();
  }
}
