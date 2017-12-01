package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class WordNetWrapperTest {
    @Before
    public void setUp() {
        StanfordPipelineProvider.getSingletonPipelineInstance();
    }

    @Test
    public void testLookUpWord() {
        WordNetWrapper wordNetWrapper = new WordNetWrapper();
        Set<String> actualSynonyms = wordNetWrapper.lookUpWords("Whom did Lance Bass marry?");
        assertTrue(actualSynonyms.contains("espouse"));
    }
}
