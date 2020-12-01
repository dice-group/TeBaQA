package de.uni.leipzig.tebaqa.tebaqacommons.util;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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

    private static double getJWSim(String s, String s2) {
        if (s == null || s2 == null) {
            return 0;
        }
        return StringUtils.getJaroWinklerDistance(s, s2);
    }

    private static double getJWDistance(String s, String s2) {
        return (1.0 - (getJWSim(s, s2) * ((double) Math.min(s2.length(), s.length()) / Math.max(s2.length(), s.length()))));
    }

    public static double getDistanceScore(String s, String s2) {
//        return (getJWDistance(s, s2) + nGramDistance(s, s2)) / 2;
        return (0.8 * getJWDistance(s, s2)) + (0.2 * nGramDistance(s, s2));
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

    private static Set<String> ngrams(String input) {
        Set<String> nGrams = new HashSet<>();
        try {
            Reader reader = new StringReader(input);
            NGramTokenizer gramTokenizer = new NGramTokenizer(2, 2);
            gramTokenizer.setReader(reader);
            CharTermAttribute charTermAttribute = gramTokenizer.addAttribute(CharTermAttribute.class);
            gramTokenizer.reset();


            while (gramTokenizer.incrementToken()) {
                String token = charTermAttribute.toString();
                //Do something
                nGrams.add(token);
            }
            gramTokenizer.end();
            gramTokenizer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return nGrams;
    }

    private static double nGramSim(String s1, String s2) {
        Set<String> ngrams1 = ngrams(s1);
        Set<String> ngrams2 = ngrams(s2);

//        int denominator = Math.max(ngrams1.size(), ngrams2.size());
//        return (double) Sets.intersection(ngrams1, ngrams2).size() / Sets.union(ngrams1, ngrams2).size();
        return (double) Sets.intersection(ngrams1, ngrams2).size() / Math.max(ngrams1.size(), ngrams2.size());
    }

    private static double nGramDistance(String s1, String s2) {
        return 1.0 - nGramSim(s1, s2);
    }

}
