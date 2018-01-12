package de.uni.leipzig.tebaqa.model;

import edu.cmu.lti.jawjaw.pobj.POS;
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
        Double similarity = wordNetWrapper.semanticWordSimilarity("war", POS.n, "battle", POS.n);
        Double expected = 0.5;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityWithInvalidPOS() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("war", POS.n, "battle", POS.a);
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarity2() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("movie", POS.n, "film", POS.n);
        Double expected = 1.0;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarity3() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("mayor", POS.n, "party", POS.n);
        Double expected = 0.2;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityWithWordgroup() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("bodyOfWater", "river", POS.n);
        Double expected = 0.3333333333333333;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityWithWordgroupBetweenDifferentPOS() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("deathDate", "die", POS.v);
        Double expected = 0.3333333333333333;
        assertEquals(expected, similarity);
    }

    @Test
    public void testJwiSimilarityIgnoresStopwords() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Double similarity = wordNetWrapper.semanticWordSimilarity("deathDate", "be", POS.v);
        Double expected = 0.0;
        assertEquals(expected, similarity);
    }
}
