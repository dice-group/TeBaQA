package de.uni.leipzig.tebaqa.helper;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class QueryMappingTest {

    @Test
    public void testCreateQueryPattern() throws Exception {
        Query query = QueryFactory.create("PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:sourceCountry ?uri . }");
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB");
        posSequence.put("country", "NN");
        posSequence.put("Nile", "NNP");
        QueryMapping queryMapping = new QueryMapping(posSequence, query);

        assertEquals("PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { ^1 ^2 ?uri . }", queryMapping.getQueryPattern());

    }

    @Test
    public void testCreateQueryPatternValues() throws Exception {
        Query query = QueryFactory.create("PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:sourceCountry ?uri . }");
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB");
        posSequence.put("country", "NN");
        posSequence.put("Nile", "NNP");
        QueryMapping queryMapping = new QueryMapping(posSequence, query);
        List<String> expectedQueryPatternValues = new ArrayList<>();
        expectedQueryPatternValues.add("NNP_0");
        expectedQueryPatternValues.add("NN_0");

        assertEquals(expectedQueryPatternValues, queryMapping.getPatternValues());
    }
}