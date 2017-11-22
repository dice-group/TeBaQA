package de.uni.leipzig.tebaqa.helper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SPARQLUtilitiesTest {
    @Test
    public void resolveNamespacesTestWithDotAfterEntity() throws Exception {
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
    public void resolveNamespacesTest() throws Exception {
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
    public void resolveNamespacesTestTwoTriples() throws Exception {
        String s = SPARQLUtilities.resolveNamespaces("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . res:Nile dbo:city ?uri . }");
        String expected = "SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Nile> <http://dbpedia.org/ontology/city> ?uri . <http://dbpedia.org/resource/Nile> <http://dbpedia.org/ontology/city> ?uri . }";
        assertEquals(expected, s);
    }


    @Test
    public void resolveNamespacesTestWithSmallerChar() throws Exception {
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
    public void resolveNamespacesTestWithGreaterChar() throws Exception {
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
    public void testResolveNamespacesWithNoNS() throws Exception {
        String s = SPARQLUtilities.resolveNamespaces("SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/routeStart> <http://dbpedia.org/resource/Piccadilly>. } ");
        String expected = "SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/routeStart> <http://dbpedia.org/resource/Piccadilly>. }";
        assertEquals(expected, s);
    }
}