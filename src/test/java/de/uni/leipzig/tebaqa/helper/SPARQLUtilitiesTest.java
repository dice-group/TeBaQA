package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.model.ResultsetBinding;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SPARQLUtilitiesTest {
    @Test
    public void resolveNamespacesTestWithDotAfterEntity() {
        String s = SPARQLUtilities.resolveNamespaces("PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                "PREFIX res: <http://dbpedia.org/resource/>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT DISTINCT ?uri \n" +
                "WHERE {\n" +
                "        ?x rdf:type dbo:Album.\n" +
                "        ?x dbo:artist res:Elvis_Presley.\n" +
                "        ?x dbo:releaseDate ?y.\n" +
                "        ?x dbo:recordLabel ?uri.\n" +
                "}\n" +
                "ORDER BY ASC(?y) \n" +
                "OFFSET 0 LIMIT 1");
        String expected = "SELECT DISTINCT ?uri " +
                "WHERE {" +
                " ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Album>." +
                " ?x <http://dbpedia.org/ontology/artist> <http://dbpedia.org/resource/Elvis_Presley>." +
                " ?x <http://dbpedia.org/ontology/releaseDate> ?y." +
                " ?x <http://dbpedia.org/ontology/recordLabel> ?uri." +
                " }" +
                " ORDER BY ASC(?y)" +
                " OFFSET 0 LIMIT 1";
        assertEquals(expected, s);
    }

    @Test
    public void resolveNamespacesTest() {
        String s = SPARQLUtilities.resolveNamespaces("PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                "PREFIX res: <http://dbpedia.org/resource/>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT DISTINCT ?uri \n" +
                "WHERE {\n" +
                "        ?x rdf:type dbo:Album .\n" +
                "        ?x dbo:artist res:Elvis_Presley .\n" +
                "        ?x dbo:releaseDate ?y .\n" +
                "        ?x dbo:recordLabel ?uri .\n" +
                "}\n" +
                "ORDER BY ASC(?y) \n" +
                "OFFSET 0 LIMIT 1");
        String expected = "SELECT DISTINCT ?uri " +
                "WHERE {" +
                " ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Album> ." +
                " ?x <http://dbpedia.org/ontology/artist> <http://dbpedia.org/resource/Elvis_Presley> ." +
                " ?x <http://dbpedia.org/ontology/releaseDate> ?y ." +
                " ?x <http://dbpedia.org/ontology/recordLabel> ?uri ." +
                " }" +
                " ORDER BY ASC(?y)" +
                " OFFSET 0 LIMIT 1";
        assertEquals(expected, s);
    }

    @Test
    public void resolveNamespacesTestTwoTriples() {
        String s = SPARQLUtilities.resolveNamespaces("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . res:Nile dbo:city ?uri . }");
        String expected = "SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Nile> <http://dbpedia.org/ontology/city> ?uri . <http://dbpedia.org/resource/Nile> <http://dbpedia.org/ontology/city> ?uri . }";
        assertEquals(expected, s);
    }


    @Test
    public void resolveNamespacesTestWithSmallerChar() {
        String s = SPARQLUtilities.resolveNamespaces("ASK WHERE { " +
                "?class_ ?property_ ?x . " +
                "?class_ ?property_ ?y . " +
                "FILTER (?x < ?y) . " +
                "VALUES (?class_) {(<http://dbpedia.org/resource/Breaking_Bad>) (<http://dbpedia.org/ontology/Game>)} " +
                "VALUES (?property_) {(<http://dbpedia.org/ontology/episode>)}" +
                "}");
        String expected = "ASK WHERE { " +
                "?class_ ?property_ ?x . " +
                "?class_ ?property_ ?y . " +
                "FILTER (?x < ?y) . " +
                "VALUES (?class_) {(<http://dbpedia.org/resource/Breaking_Bad>) (<http://dbpedia.org/ontology/Game>)} " +
                "VALUES (?property_) {(<http://dbpedia.org/ontology/episode>)}" +
                "}";
        assertEquals(expected, s);
    }

    @Test
    public void resolveNamespacesTestWithGreaterChar() {
        String s = SPARQLUtilities.resolveNamespaces("ASK WHERE { " +
                "?class_ ?property_ ?x . " +
                "?class_ ?property_ ?y . " +
                "FILTER (?x > ?y) . " +
                "VALUES (?class_) {(<http://dbpedia.org/resource/Breaking_Bad>) (<http://dbpedia.org/ontology/Game>)} " +
                "VALUES (?property_) {(<http://dbpedia.org/ontology/episode>)}" +
                "}");
        String expected = "ASK WHERE { " +
                "?class_ ?property_ ?x . " +
                "?class_ ?property_ ?y . " +
                "FILTER (?x > ?y) . " +
                "VALUES (?class_) {(<http://dbpedia.org/resource/Breaking_Bad>) (<http://dbpedia.org/ontology/Game>)} " +
                "VALUES (?property_) {(<http://dbpedia.org/ontology/episode>)}" +
                "}";
        assertEquals(expected, s);
    }

    @Test
    public void testResolveNamespacesWithNoNS() {
        String s = SPARQLUtilities.resolveNamespaces("SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/routeStart> <http://dbpedia.org/resource/Piccadilly>. } ");
        String expected = "SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/routeStart> <http://dbpedia.org/resource/Piccadilly>. } ";
        assertEquals(expected, s);
    }

    @Test
    public void testResultSetTypeSingleResource() {
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery("SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/routeStart> <http://dbpedia.org/resource/Piccadilly>. } ");
        assertTrue(sparqlResultSets.size() == 1);
        SPARQLResultSet  sparqlResultSet = sparqlResultSets.get(0);
        assertEquals(SPARQLResultSet.SINGLE_ANSWER, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeNumber() {
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery("SELECT (COUNT(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . }");
        assertTrue(sparqlResultSets.size() == 1);
        SPARQLResultSet  sparqlResultSet = sparqlResultSets.get(0);
        assertEquals(SPARQLResultSet.NUMBER_ANSWER_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeNumberWithExponentialNumber() {
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbr: <http://dbpedia.org/resource/> SELECT ?tarea WHERE { dbr:North_Rhine-Westphalia dbo:areaTotal ?tarea }");
        assertTrue(sparqlResultSets.size() == 1);
        SPARQLResultSet  sparqlResultSet = sparqlResultSets.get(0);
        assertEquals(SPARQLResultSet.NUMBER_ANSWER_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeList() {
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery("SELECT DISTINCT ?uri WHERE {  ?x <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/William_Shatner> .  ?x <http://dbpedia.org/ontology/starring> ?uri . } ");
        SPARQLResultSet  sparqlResultSet = sparqlResultSets.get(0);
        assertTrue(sparqlResultSets.size() == 1);
        assertEquals(SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeBoolean() {
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery("ASK WHERE { <http://dbpedia.org/resource/Neymar> <http://dbpedia.org/ontology/team> <http://dbpedia.org/resource/Real_Madrid_C.F.> . }");
        SPARQLResultSet  sparqlResultSet = sparqlResultSets.get(0);
        assertTrue(sparqlResultSets.size() == 1);
        assertEquals(SPARQLResultSet.BOOLEAN_ANSWER_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeUnknown() {
        List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery("SELECT DISTINCT ?uri WHERE {  ?x <http://dbpedia.org/ontology/fooBar> <http://dbpedia.org/resource/William_Shatner> .  ?x <http://dbpedia.org/ontology/starring> ?uri . } ");
        SPARQLResultSet  sparqlResultSet = sparqlResultSets.get(0);
        assertTrue(sparqlResultSets.size() == 1);
        assertEquals(SPARQLResultSet.UNKNOWN_ANSWER_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testGetPageRankWithResource() {
        Double pageRank = SPARQLUtilities.getPageRank("http://dbpedia.org/resource/William_Shatner");
        assertEquals(26.3249, pageRank, 5);
    }

    @Test
    public void testGetPageRankWithOntology() {
        Double pageRank = SPARQLUtilities.getPageRank("http://dbpedia.org/ontology/Bird");
        assertEquals(Double.MAX_VALUE, pageRank, 0);
    }

    @Test
    public void testGetRedirect() {
        String actual = SPARQLUtilities.getRedirect("http://dbpedia.org/resource/G._W._F._Hegel");
        assertEquals("http://dbpedia.org/resource/Georg_Wilhelm_Friedrich_Hegel", actual);
    }

    @Test
    public void testGetRedirect2() {
        String actual = SPARQLUtilities.getRedirect("http://dbpedia.org/resource/Carolina_Reaper");
        assertEquals("http://dbpedia.org/resource/Carolina_Reaper", actual);
    }

    @Test
    public void testReplaceWithWildcard() {
        String actual = SPARQLUtilities.replaceWithWildcard("SELECT DISTINCT ?uri WHERE { res:Brooklyn_Bridge dbo:crosses ?uri }");
        assertEquals("SELECT DISTINCT * WHERE { res:Brooklyn_Bridge dbo:crosses ?uri }", actual);
    }

    @Test
    public void testReplaceWithWildcard2() {
        String actual = SPARQLUtilities.replaceWithWildcard("SELECT ?uri WHERE { res:Brooklyn_Bridge dbo:crosses ?uri }");
        assertEquals("SELECT * WHERE { res:Brooklyn_Bridge dbo:crosses ?uri }", actual);
    }

    @Test
    public void testIsNumeric() {
        assertTrue(SPARQLUtilities.isNumericOrScientific("1"));
    }

    @Test
    public void testIsNumericWithNegative() {
        assertTrue(SPARQLUtilities.isNumericOrScientific("-1.0"));
    }

    @Test
    public void testIsNumericWithDot() {
        assertTrue(SPARQLUtilities.isNumericOrScientific("44.8"));
    }

    @Test
    public void testIsNumericWithScientific() {
        assertTrue(SPARQLUtilities.isNumericOrScientific("3.40841e+10"));
    }

    @Test
    public void testDetermineAnswerType() {
        ResultsetBinding rs = new ResultsetBinding();
        Set<String> result = new HashSet<>();
        result.add("1616-04-23");
        result.add("1616-4-23");
        result.add("1616-04");
        result.add("1616-4");
        result.add("1616");
        rs.setResult(result);
        assertEquals(SPARQLResultSet.DATE_ANSWER_TYPE, SPARQLUtilities.determineAnswerType(rs));
    }

    @Test
    public void testDetermineAnswerType2() {
        ResultsetBinding rs = new ResultsetBinding();
        Set<String> result = new HashSet<>();
        result.add("true");
        rs.setResult(result);
        assertEquals(SPARQLResultSet.BOOLEAN_ANSWER_TYPE, SPARQLUtilities.determineAnswerType(rs));
    }

    @Test
    public void testDetermineAnswerType3() {
        ResultsetBinding rs = new ResultsetBinding();
        Set<String> result = new HashSet<>();
        result.add("false");
        rs.setResult(result);
        assertEquals(SPARQLResultSet.BOOLEAN_ANSWER_TYPE, SPARQLUtilities.determineAnswerType(rs));
    }

    @Test
    public void testTransformCountToStarQuery() {
        final String actual = SPARQLUtilities.transformCountToStarQuery("SELECT (count(distinct *) AS ?c) WHERE  { ?s ?p ?o }");
        assertEquals("SELECT *  WHERE { ?s ?p ?o }", actual);
    }

    @Test
    public void testTetrieveBiningsWithCountQuery() {
        final List<ResultsetBinding> bindings = SPARQLUtilities.retrieveBinings("SELECT  (count(distinct *) AS ?c)\n" +
                "WHERE\n" +
                "  { ?uri      ?property_0  ?class_0 ;\n" +
                "              ?property_1  ?year .\n" +
                "    ?class_1  ?property_2  ?year\n" +
                "    VALUES ?class_0 { <http://dbpedia.org/ontology/City> <http://dbpedia.org/resource/City> <http://dbpedia.org/ontology/Population> <http://dbpedia.org/resource/Mexico_City> <http://dbpedia.org/resource/Population> <http://dbpedia.org/resource/Mexico> }\n" +
                "    VALUES ?class_1 { <http://dbpedia.org/ontology/City> <http://dbpedia.org/resource/City> <http://dbpedia.org/ontology/Population> <http://dbpedia.org/resource/Mexico_City> <http://dbpedia.org/resource/Population> <http://dbpedia.org/resource/Mexico> }\n" +
                "    VALUES ?class_2 { <http://dbpedia.org/ontology/City> <http://dbpedia.org/resource/City> <http://dbpedia.org/ontology/Population> <http://dbpedia.org/resource/Mexico_City> <http://dbpedia.org/resource/Population> <http://dbpedia.org/resource/Mexico> }\n" +
                "    VALUES ?property_0 { <http://dbpedia.org/property/city> <http://dbpedia.org/property/population> <http://dbpedia.org/ontology/city> <http://dbpedia.org/property/populationTotal> <http://dbpedia.org/ontology/populationTotal> <http://dbpedia.org/ontology/population> <http://dbpedia.org/property/populationOf> }\n" +
                "    VALUES ?property_1 { <http://dbpedia.org/property/city> <http://dbpedia.org/property/population> <http://dbpedia.org/ontology/city> <http://dbpedia.org/property/populationTotal> <http://dbpedia.org/ontology/populationTotal> <http://dbpedia.org/ontology/population> <http://dbpedia.org/property/populationOf> }\n" +
                "    VALUES ?property_2 { <http://dbpedia.org/property/city> <http://dbpedia.org/property/population> <http://dbpedia.org/ontology/city> <http://dbpedia.org/property/populationTotal> <http://dbpedia.org/ontology/populationTotal> <http://dbpedia.org/ontology/population> <http://dbpedia.org/property/populationOf> }\n" +
                "    FILTER ( concat(?uri, ?property_0, ?class_0) != concat(?uri, ?property_1, ?year) )\n" +
                "    FILTER ( concat(?uri, ?property_0, ?class_0) != concat(?class_1, ?property_2, ?year) )\n" +
                "    FILTER ( concat(?uri, ?property_1, ?year) != concat(?uri, ?property_0, ?class_0) )\n" +
                "    FILTER ( concat(?uri, ?property_1, ?year) != concat(?class_1, ?property_2, ?year) )\n" +
                "    FILTER ( concat(?class_1, ?property_2, ?year) != concat(?uri, ?property_0, ?class_0) )\n" +
                "    FILTER ( concat(?class_1, ?property_2, ?year) != concat(?uri, ?property_1, ?year) )\n" +
                "  }");
        assertEquals(1, bindings.size());
    }
}