package de.uni.leipzig.tebaqa.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueryBuilderTest {

    @Test
    public void testBuildQuery() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("When did Boris Becker end his active career?", " {\"1\" @\"p\" \"2\"}");
        String query = queryBuilder.getQuery();
        assertEquals("SELECT DISTINCT ?d WHERE { <http://dbpedia.org/resource/Boris_Becker> <http://dbpedia.org/ontology/activeYearsEndDate> ?d . }", query);
    }

}