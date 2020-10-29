package de.uni.leipzig.tebaqa.entitylinking.nlp;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class StopWordsUtil {

    private static final Map<String, Set<String>> STOP_WORD_MAP = new HashMap<>();

    static {
        Properties prop = new Properties();
        try {
            prop.load(ClassLoader.getSystemResource("stopwords.properties").openStream());
            for (String lang : prop.stringPropertyNames()) {
                loadStopwords(prop.getProperty(lang)).ifPresent(set -> STOP_WORD_MAP.put(lang, (Set) set));
            }

        } catch (IOException ex) {
            System.out.println("Properties not found");
        }
    }

    private static Optional<Set<String>> loadStopwords(String filename) {
        FileReader fileReader;
        try {
            Set<String> stopwords = new HashSet<>();
            fileReader = new FileReader(ClassLoader.getSystemResource(filename).getPath());
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String element;
            while ((element = bufferedReader.readLine()) != null)
                stopwords.add(element);
            bufferedReader.close();
            return Optional.of(stopwords);
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();

    }

    public static boolean containsOnlyStopwords(String coOccurrence, Lang lang) {
        String[] words = coOccurrence.replaceAll("[\\-.?¿!,;]", "").toLowerCase().split("\\s+");
        for (String word : words)
            if (!STOP_WORD_MAP.getOrDefault(lang.getLanguageCode(), Collections.emptySet()).contains(word))
                return false;
        return true;
    }

    public static boolean containsAnyStopword(String coOccurrence, Lang lang) {
        String[] words = coOccurrence.replaceAll("[\\-.?¿!,;]", "").toLowerCase().split("\\s+");
        for (String word : words)
            if (STOP_WORD_MAP.getOrDefault(lang.getLanguageCode(), Collections.emptySet()).contains(word)) return true;
        return false;
    }

    public List<String> generateTokens(String question, Lang lang) {
        Set<String> stopwords = STOP_WORD_MAP.getOrDefault(lang.getLanguageCode(), Collections.emptySet());
        String[] words = question.replaceAll("[\\-.?¿!,;]", "").toLowerCase().split("\\s+"); //[\\p{Alnum},\\s#\\-.]
        List<String> keywords = new ArrayList<>();
        for (String word : words)
            if (!stopwords.contains(word))
                keywords.add(word);
        return keywords;
    }

}
