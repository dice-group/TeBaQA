package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import org.assertj.core.util.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoOccurrenceEntityMapping {
    private String wordGroup;
    private String entity;
    private int size;
    private List<String> matchingWords;

    public CoOccurrenceEntityMapping(String wordGroup, String entityURI) {
        this.wordGroup = wordGroup;
        this.entity = entityURI;
        this.size = 0;
        this.matchingWords = new ArrayList<>();
        Map<String, String> lemmas = SemanticAnalysisHelper.getLemmas(wordGroup);
        String[] split = entityURI.split("/");
        String entity = split[split.length - 1];
        Set<String> wordsInWordGroup = Sets.newHashSet(Arrays.asList(wordGroup.split("\\W+")));
        for (String word : wordsInWordGroup) {
            String lemma = lemmas.get(word);
            if(lemma != null && !lemma.equalsIgnoreCase(word)){
                Pattern pattern = Pattern.compile(".*" + lemma + ".*", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(entity.toLowerCase());
                while (matcher.find()) {
                    matchingWords.add(lemma);
                    this.size++;
                }
            }
            Pattern pattern = Pattern.compile(".*" + word + ".*", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(entity.toLowerCase());
            while (matcher.find()) {
                matchingWords.add(word);
                this.size++;
            }

        }
    }

    public String getWordGroup() {
        return wordGroup;
    }

    public String getEntity() {
        return entity;
    }

    public int getSize() {
        return size;
    }

    public List<String> getMatchingWords() {
        return matchingWords;
    }

    @Override
    public String toString() {
        return "CoOccurrenceEntityMapping{" +
                "wordGroup='" + wordGroup + '\'' +
                ", entity='" + entity + '\'' +
                ", size=" + size +
                ", matchingWords=" + matchingWords +
                '}';
    }
}
