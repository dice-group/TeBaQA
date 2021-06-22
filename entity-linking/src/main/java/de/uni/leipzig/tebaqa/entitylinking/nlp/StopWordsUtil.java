package de.uni.leipzig.tebaqa.entitylinking.nlp;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StopWordsUtil {

    private static final Logger LOGGER = LogManager.getLogger(StopWordsUtil.class);
    private static final Map<String, Set<String>> STOP_WORD_MAP = new HashMap<>();

    static {
        Properties prop = new Properties();
        try {
            prop.load(StopWordsUtil.class.getClassLoader().getResourceAsStream("stopwords.properties"));
            for (String lang : prop.stringPropertyNames()) {
                loadStopwords(prop.getProperty(lang)).ifPresent(set -> STOP_WORD_MAP.put(lang, set));
            }

        } catch (IOException ex) {
            System.out.println("Properties not found");
        }
    }

    private static Optional<Set<String>> loadStopwords(String filename) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(StopWordsUtil.class.getClassLoader().getResourceAsStream(filename)), StandardCharsets.UTF_8))) {
            Set<String> stopwords = new HashSet<>();
            String element;
            while ((element = bufferedReader.readLine()) != null)
                stopwords.add(element);
            return Optional.of(stopwords);
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: " + filename);
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error("Error reading stopwords file: " + filename);
            LOGGER.error(e.getMessage());
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

    public static void main(String[] args) {
        System.out.println(StopWordsUtil.containsOnlyStopwords("the", Lang.EN));
    }
}
