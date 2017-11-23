package de.uni.leipzig.tebaqa.controller;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QueryIsomorphismTest {

    @Test
    public void testAreIsomorphTrue() throws Exception {
        assertTrue(QueryIsomorphism.areIsomorph("SELECT DISTINCT ?uri WHERE { ?x <http://dbpedia.org/property/office> <http://dbpedia.org/resource/President_of_Montenegro> . }", "SELECT DISTINCT ?x WHERE { ?x <http://dbpedia.org/property/someThing> <http://dbpedia.org/resource/A_Random_Resource> . }"));
    }

    @Test
    public void testAreIsomorphFalse() throws Exception {
        assertFalse(QueryIsomorphism.areIsomorph("SELECT DISTINCT ?uri WHERE { ?x <http://dbpedia.org/property/office> <http://dbpedia.org/resource/President_of_Montenegro> . }", "SELECT DISTINCT ?uri WHERE {  ?x <http://dbpedia.org/property/office> <http://dbpedia.org/resource/President_of_Montenegro> .  ?x <http://dbpedia.org/ontology/birthPlace> ?uri .  ?uri a <http://dbpedia.org/ontology/City> . }"));
    }
}