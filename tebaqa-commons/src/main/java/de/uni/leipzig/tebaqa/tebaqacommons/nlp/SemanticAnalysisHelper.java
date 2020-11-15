package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SemanticAnalysisHelper implements ISemanticAnalysisHelper {

    protected List<String> getFirstThreeWords(String sentence) {
        String[] words = sentence.split("\\s+");

        List<String> firstThreeWords = new ArrayList<>();
        if (words.length > 3) {
            firstThreeWords.addAll(Arrays.asList(words).subList(0, 3));
        } else {
            firstThreeWords.addAll(Arrays.asList(words));
        }

        return firstThreeWords;
    }

}
