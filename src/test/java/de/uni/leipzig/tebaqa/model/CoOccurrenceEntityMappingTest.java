package de.uni.leipzig.tebaqa.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoOccurrenceEntityMappingTest {

    @Test
    public void testGenerateMapping() {
        CoOccurrenceEntityMapping mapping = new CoOccurrenceEntityMapping("official language", "http://dbpedia.org/ontology/officialLanguage");
        assertEquals(2, mapping.getSize());
    }

    @Test
    public void testGenerateMappingWontCountNonRelatedParts() {
        CoOccurrenceEntityMapping mapping = new CoOccurrenceEntityMapping("official language", "http://dbpedia.org/ontology/deFactoLanguage");
        assertEquals(1, mapping.getSize());
    }

    @Test
    public void testGenerateMappingWontCountNonRelatedParts2() {
        CoOccurrenceEntityMapping mapping = new CoOccurrenceEntityMapping("language", "http://dbpedia.org/ontology/deFactoLanguage");
        assertEquals(1, mapping.getSize());
    }

    @Test
    public void testGenerateMappingWontCountNonRelatedParts3() {
        CoOccurrenceEntityMapping mapping = new CoOccurrenceEntityMapping("foo language", "http://dbpedia.org/ontology/titleLanguage");
        assertEquals(1, mapping.getSize());
    }

    @Test
    public void testGenerateMappingWontCountNonRelatedParts4() {
        CoOccurrenceEntityMapping mapping = new CoOccurrenceEntityMapping("language foo", "http://dbpedia.org/ontology/titleLanguage");
        assertEquals(1, mapping.getSize());
    }

    @Test
    public void testGenerateMappingDetectsLemmas() {
        CoOccurrenceEntityMapping mapping = new CoOccurrenceEntityMapping("electronics companies", "http://dbpedia.org/ontology/Company");
        assertEquals(1, mapping.getSize());
    }
}
