package de.uni.leipzig.tebaqa.controller;

import de.uni.leipzig.tebaqa.model.Modifier;
import de.uni.leipzig.tebaqa.model.QueryTemplate;
import org.apache.jena.query.QueryParseException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryTemplatesBuilderTest {

    private static String UNPARSEABLE_QUERY = "some random string";

    @Test
    public void getModifiersWithFilterTest() throws Exception {
        ArrayList<String> queries = new ArrayList<>();
        String simpleSelectWithFilter = "SELECT DISTINCT ?uri WHERE { ?uri a " +
                "<http://dbpedia.org/ontology/BasketballPlayer> . ?uri <http://dbpedia.org/ontology/height> ?n . " +
                "FILTER (?n > 2.0) }";
        queries.add(simpleSelectWithFilter);
        QueryTemplatesBuilder queryTemplatesBuilder = new QueryTemplatesBuilder(queries);
        Modifier correctModifier = new Modifier("FILTER ( ? > ? )");
        Set<Modifier> modifiersToTest = queryTemplatesBuilder.getQueryTemplates().get(0).getModifiers();

        assertEquals(correctModifier, modifiersToTest.toArray()[0]);
        assertEquals(1, modifiersToTest.size());
    }

    @Test
    public void testAskQuery() throws Exception {
        ArrayList<String> queries = new ArrayList<>();
        String simpleAskWithFilter = "ASK WHERE { ?uri a " +
                "<http://dbpedia.org/ontology/BasketballPlayer> . ?uri <http://dbpedia.org/ontology/height> ?n . " +
                "FILTER (?n > 2.0) }";
        queries.add(simpleAskWithFilter);
        QueryTemplatesBuilder queryTemplatesBuilder = new QueryTemplatesBuilder(queries);
        Modifier correctModifier = new Modifier("FILTER ( ? > ? )");
        Set<Modifier> modifiersToTest = queryTemplatesBuilder.getQueryTemplates().get(0).getModifiers();

        assertEquals(1, modifiersToTest.size());
        assertEquals(correctModifier, modifiersToTest.toArray()[0]);
    }

    @Test
    public void testUnionModifier() throws Exception {
        ArrayList<String> queries = new ArrayList<>();
        String simpleAskWithFilter = "SELECT DISTINCT ?uri WHERE { { ?uri <http://dbpedia.org/ontology/field>" +
                "<http://dbpedia.org/resource/Computer_science> . } " +
                "UNION { ?uri <http://purl.org/dc/elements/1.1/description> ?s . " +
                "FILTER regex(?s,'computer scientist','i') } ?uri <http://dbpedia.org/ontology/award> " +
                "<http://dbpedia.org/resource/Academy_Awards> . }  \n";
        queries.add(simpleAskWithFilter);
        QueryTemplatesBuilder queryTemplatesBuilder = new QueryTemplatesBuilder(queries);
        Set<Modifier> modifiersToTest = queryTemplatesBuilder.getQueryTemplates().get(0).getModifiers();

        assertEquals(1, modifiersToTest.size());
        assertTrue(modifiersToTest.contains(new Modifier("{ ? <> <> } UNION { ? <> ? FILTER regex( ? , ? , ? ) }")));
    }

    @Test
    public void testNonSelectAskQuery() throws Exception {
        ArrayList<String> queries = new ArrayList<>();
        String simpleDescribeQuery = "DESCRIBE <http://example.org/>";
        queries.add(simpleDescribeQuery);
        QueryTemplatesBuilder queryTemplatesBuilder = new QueryTemplatesBuilder(queries);
        List<QueryTemplate> queryTemplates = queryTemplatesBuilder.getQueryTemplates();

        assert queryTemplates.isEmpty();
    }

    //TODO modifier values is not recognized well
    @Test
    public void testNestedSelect() throws Exception {
        ArrayList<String> queries = new ArrayList<>();
        String nestedSelectQuery = "select ?x ?y where {values ?x { 1 2 3 } " +
                "{ select ?y where {  values ?y { 5 6 7 8 } } limit 2 }}";
        queries.add(nestedSelectQuery);
        QueryTemplatesBuilder queryTemplatesBuilder = new QueryTemplatesBuilder(queries);
        Set<Modifier> modifiersToTest = queryTemplatesBuilder.getQueryTemplates().get(0).getModifiers();

        assertEquals(3, modifiersToTest.size());
        assertTrue(modifiersToTest.contains(new Modifier("VALUES ? { ? ? ? }")));
        assertTrue(modifiersToTest.contains(new Modifier("VALUES ? { ? ? ? ?}")));
        assertTrue(modifiersToTest.contains(new Modifier("LIMIT ?")));
    }

    @Test
    public void testGetModifiersHandlesMultipleModifiers() throws Exception {
        ArrayList<String> queries = new ArrayList<>();
        String selectExtendedFilter = "SELECT DISTINCT ?n WHERE " +
                "{ ?x a <http://dbpedia.org/ontology/BasketballPlayer> . ?x <http://dbpedia.org/ontology/league> " +
                "<http://dbpedia.org/resource/National_Basketball_Association> . ?x <http://dbpedia.org/ontology/height> " +
                "?n . FILTER NOT EXISTS { ?x <http://dbpedia.org/ontology/activeYearsEndYear> ?d . } } " +
                "ORDER BY ASC(?n) OFFSET 0 LIMIT 1";
        queries.add(selectExtendedFilter);
        QueryTemplatesBuilder queryTemplatesBuilder = new QueryTemplatesBuilder(queries);
        Set<Modifier> modifiersToTest = queryTemplatesBuilder.getQueryTemplates().get(0).getModifiers();

        assertEquals(4, modifiersToTest.size());
        assertTrue(modifiersToTest.contains(new Modifier("FILTER NOT EXISTS { ? <> ? }")));
        assertTrue(modifiersToTest.contains(new Modifier("ORDER BY ASC( ? )")));
        assertTrue(modifiersToTest.contains(new Modifier("OFFSET ?")));
        assertTrue(modifiersToTest.contains(new Modifier("LIMIT ?")));
    }

    @Test(expected = QueryParseException.class)
    public void unparseableQuery() throws Exception {
        ArrayList<String> queries = new ArrayList<>();
        queries.add(UNPARSEABLE_QUERY);
        QueryTemplatesBuilder queryTemplatesBuilder = new QueryTemplatesBuilder(queries);
    }

    @Test
    public void testSeperatorAfterQuestionMarkWithoutSpaceStays() throws Exception {
        ArrayList<String> queries = new ArrayList<>();
        String queryNoSpaceBeforeDot = "SELECT DISTINCT ?uri WHERE { ?uri a " +
                "<http://dbpedia.org/ontology/BasketballPlayer> . ?uri <http://dbpedia.org/ontology/height> ?n . " +
                " FILTER NOT EXISTS { ?uri <http://dbpedia.org/ontology/activeYearsEndYear> ?uri. " +
                "?uri <http://dbpedia.org/ontology/height> ?n } }";
        queries.add(queryNoSpaceBeforeDot);
        QueryTemplatesBuilder queryTemplatesBuilder = new QueryTemplatesBuilder(queries);
        Modifier correctModifier = new Modifier("FILTER NOT EXISTS { ? <> ? ; <> ? }");
        Set<Modifier> modifiersToTest = queryTemplatesBuilder.getQueryTemplates().get(0).getModifiers();

        assertEquals(1, modifiersToTest.size());
        assertEquals(correctModifier, modifiersToTest.toArray()[0]);
    }
}
