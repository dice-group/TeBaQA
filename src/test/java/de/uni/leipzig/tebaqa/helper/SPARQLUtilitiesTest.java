package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        String expected = "SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/routeStart> <http://dbpedia.org/resource/Piccadilly>. }";
        assertEquals(expected, s);
    }

    @Test
    public void testResultSetTypeSingleResource() {
        SPARQLResultSet sparqlResultSet = SPARQLUtilities.executeSPARQLQuery("SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/routeStart> <http://dbpedia.org/resource/Piccadilly>. } ");
        assertEquals(SemanticAnalysisHelper.SINGLE_RESOURCE_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeNumber() {
        SPARQLResultSet sparqlResultSet = SPARQLUtilities.executeSPARQLQuery("SELECT (COUNT(DISTINCT ?x) as ?c) WHERE {  <http://dbpedia.org/resource/Turkmenistan> <http://dbpedia.org/ontology/language> ?x . }");
        assertEquals(SemanticAnalysisHelper.NUMBER_ANSWER_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeList() {
        SPARQLResultSet sparqlResultSet = SPARQLUtilities.executeSPARQLQuery("SELECT DISTINCT ?uri WHERE {  ?x <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/William_Shatner> .  ?x <http://dbpedia.org/ontology/starring> ?uri . } ");
        assertEquals(SemanticAnalysisHelper.LIST_ANSWER_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeBoolean() {
        SPARQLResultSet sparqlResultSet = SPARQLUtilities.executeSPARQLQuery("ASK WHERE { <http://dbpedia.org/resource/Neymar> <http://dbpedia.org/ontology/team> <http://dbpedia.org/resource/Real_Madrid_C.F.> . }");
        assertEquals(SemanticAnalysisHelper.BOOLEAN_ANSWER_TYPE, sparqlResultSet.getType());
    }

    @Test
    public void testResultSetTypeUnknown() {
        SPARQLResultSet sparqlResultSet = SPARQLUtilities.executeSPARQLQuery("SELECT DISTINCT ?uri WHERE {  ?x <http://dbpedia.org/ontology/fooBar> <http://dbpedia.org/resource/William_Shatner> .  ?x <http://dbpedia.org/ontology/starring> ?uri . } ");
        assertEquals(SemanticAnalysisHelper.UNKNOWN_ANSWER_TYPE, sparqlResultSet.getType());
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
}