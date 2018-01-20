package de.uni.leipzig.tebaqa.model;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WordNetWrapperTest {

    @Test
    public void testLookUpWord() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Set<String> actualSynonyms = wordNetWrapper.lookUpWords("Whom did Lance Bass marry?");
        assertTrue(actualSynonyms.contains("espouse"));
    }

    @Test
    public void testJwiSimilarity() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("war", "battle");
        Double expected = 0.8888888888888888;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarity2() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("movie", "film");
        Double expected = 1.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarity3() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("mayor", "party");
        Double expected = 0.2;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityWithWordgroup() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("bodyOfWater", "river");
        Double expected = 0.42857142857142855;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityWithWordgroupBetweenDifferentPOS() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("deathDate", "die");
        Double expected = 0.25;
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
    public void testJwiSimilarity4() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("president", "spouse");
        Double expected = 0.6666666666666666;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarity5() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord("firstAscentPerson", "climb");
        Double expected = 1.06;
        assertEquals(expected, similarity);
    }
}
