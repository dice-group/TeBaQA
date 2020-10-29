package de.uni.leipzig.tebaqa.tebaqacommons.util;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static java.lang.String.join;

public class TextUtilities {

    public final static String NON_WORD_CHARACTERS_REGEX = "[^a-zA-Z0-9_äÄöÖüÜ']";

    public static String trim(String s) {
        return s.trim().replaceAll("\n", " ").replaceAll("\\s{2,}", " ").trim();
    }

    public static int countWords(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        List<String> words = new ArrayList<>(Arrays.asList(s.split(NON_WORD_CHARACTERS_REGEX)));
        words.removeIf(String::isEmpty);
        return words.size();
    }


    public static List<String> getNeighborCoOccurrencePermutations(List<String> words) {
        List<String> permutations = new ArrayList<>();
        for (int i = 0; i <= words.size(); i++) {
            for (int y = 1; y <= words.size() - i; y++) {
                if (y - i < 6) {
                    permutations.add(join(" ", words.subList(i, i + y)));
                }
            }
        }
        return permutations;
    }

    public static boolean isDependent(String coOccurrence, SemanticGraph graph) {
        String[] words = coOccurrence.replace("’s", "").split("\\s+");
        if (words.length == 1 || words.length == 2 || coOccurrence.contains(" by")) return true;
        boolean depend = true;
        Set<String> unmatched = new HashSet<>();
        unmatched.addAll(Arrays.asList());
        for (int i = 0; i < words.length && depend; i++) {
            List<IndexedWord> words1 = graph.getAllNodesByWordPattern(words[i]);
            boolean isdependant = false;
            for (int j = 0; j < words.length && !isdependant; j++) {
                if (j != i) {
                    List<IndexedWord> words2 = graph.getAllNodesByWordPattern(words[j]);
                    for (int k = 0; k < words1.size() && !isdependant; k++) {
                        for (int l = 0; l < words2.size() && !isdependant; l++) {
                            if (graph.containsEdge(words1.get(k), words2.get(l)) || graph.containsEdge(words2.get(l), words1.get(k)))
                                isdependant = true;
                        }
                    }

                }
            }
            if (!isdependant && words[i].length() > 1 && !words[i].equals("by")) depend = false;
        }
        return depend;
    }

    public static double getJWSim(String s, String s2) {
        if (s == null || s2 == null) {
            return 0;
        }
        double jaroWinklerDistance = StringUtils.getJaroWinklerDistance(s, s2);
//        int lfd = StringUtils.getLevenshteinDistance(s2, s);
//        return ((double) lfd) / (Math.max(s2.length(), s.length()));
        return jaroWinklerDistance;
    }

    public static double getLevenshteinRatio(String s, String s2) {
        if (s == null || s2 == null) {
            return 1;
        }
        int lfd = StringUtils.getLevenshteinDistance(s2, s);
        return ((double) lfd) / (Math.max(s2.length(), s.length()));
    }

    public double calculateAverageLevensteinRatioByWord(String val1, String val2) {
        String[] wordsVal1 = val1.split(" ");
        String[] wordsVal2 = val2.split(" ");
        double sum = 0;
        String[] maxArray;
        String[] minArray;
        if (wordsVal1.length > wordsVal2.length) {
            maxArray = wordsVal1;
            minArray = wordsVal2;
        } else {
            maxArray = wordsVal2;
            minArray = wordsVal1;
        }
        int diff = maxArray.length - minArray.length;

        for (String word1 : minArray) {
            if (!word1.contains("(")) {
                double min = 1;
                for (String word2 : maxArray) {
                    double levenstheinscore = getLevenshteinRatio(word1, word2);
                    if (levenstheinscore < min) min = levenstheinscore;
                }
                sum += min;
            }
        }
        return (sum / maxArray.length) + diff * 0.1;

    }

    public static void main(String[] args) {
        System.out.println(getJWSim("height", "height"));
    }
}
