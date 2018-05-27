package de.uni.leipzig.tebaqa.model;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WordNetWrapperTest {

    @Test
    public void testLookUpWord() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Map<String, String> actualSynonyms = wordNetWrapper.lookUpWords("Whom did Lance Bass marry?");
        assertTrue(actualSynonyms.keySet().contains("espouse"));
    }

    @Test
    public void testJwiSimilarityRelated() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("war", "battle");
        Double expected = 16.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityRelated2() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("movie", "film");
        Double expected = 16.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityRelated3() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("mayor", "party");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityWithWordgroup() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("bodyOfWater", "river");
        Double expected = 5.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityRelated4() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("deathDate", "die");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityIgnoresStopwords() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("deathDate", "be");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityIgnoresStopwords2() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("creator", "do");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityRelated6() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("firstAscentPerson", "climb");
        Double expected = 16.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityRelated7() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("daughter", "child");
        Double expected = 2.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityRelated8() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("wife", "spouse");
        Double expected = 4.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityRelated9() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("marry", "spouse");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityUnrelated2() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("numberOfPages", "give");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityUnrelated3() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("oscar", "description");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityUnrelated4() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("languageFamily", "Urdu");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityUnrelated5() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("Guy", "Film");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityUnrelated6() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("numberOfPages", "peace");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityUnrelated7() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("Queen", "artist");
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityUnrelated8() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("parent", "spouse");
        Double expected = 2.0;
        assertEquals(expected, similarity);
    }
}
