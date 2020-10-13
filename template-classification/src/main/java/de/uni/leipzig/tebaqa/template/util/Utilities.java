package de.uni.leipzig.tebaqa.template.util;

import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    private static final Logger log = Logger.getLogger(Utilities.class);

    public static Pattern BETWEEN_CURLY_BRACES = Pattern.compile("\\{(.*?)\\}");
    public static Pattern ARGUMENTS_BETWEEN_SPACES = Pattern.compile("\\S+");

    public static List<String> extractTriples(String query) {
        List<String> triples = new ArrayList<>();
        Matcher curlyBracesMatcher = BETWEEN_CURLY_BRACES.matcher(query);
        while (curlyBracesMatcher.find()) {
            String group = curlyBracesMatcher.group().replace("{", "").replace("}", "");
            int lastFoundTripleIndex = 0;
            for (int i = 0; i < group.length(); i++) {
                char currentChar = group.charAt(i);
                //TODO handle triples with predicate-object lists and object lists; see: https://stackoverflow.com/a/18214862/2514164
                if (Character.compare(currentChar, '.') == 0
                        || Character.compare(currentChar, ';') == 0
                        || Character.compare(currentChar, ',') == 0) {
                    String possibleTriple = group.substring(lastFoundTripleIndex, i);
                    int openingAngleBrackets = StringUtils.countMatches(possibleTriple, "<");
                    int closingAngleBrackets = StringUtils.countMatches(possibleTriple, ">");
                    if (openingAngleBrackets == closingAngleBrackets) {
                        lastFoundTripleIndex = i;
                        String t = removeDotsFromStartAndEnd(possibleTriple);
                        t = fixCompoundPattern(t, triples);
                        if (!t.isEmpty()) {
                            triples.add(t);
                        }
                    }
                }
            }
            //Add the last triple which may not end with a .
            String t = removeDotsFromStartAndEnd(group.substring(lastFoundTripleIndex));
            t = fixCompoundPattern(t, triples);
            if (!t.isEmpty()) {
                triples.add(t);
            }
        }

        return triples;
    }

    private static String fixCompoundPattern(String currentPattern, List<String> previousTriples) {
        // To handle compound patterns which express two patterns with same subject with a semicolon
        // e.g. ?cs res/0 res/1 ; ?rel res/2 ; res/3 ?tel
        if (currentPattern.startsWith(";") && previousTriples.size() > 0) {
            String previousTriple = previousTriples.get(previousTriples.size() - 1); // Get previous triple to retrieve subject from it
            String previousTripleSubject = previousTriple.split("\\s{1,}")[0];

            // Replace leading semicolon in current triple with previous triple's subject
            currentPattern = previousTripleSubject + currentPattern.substring(1);
        }

        return currentPattern;
    }

    private static String removeDotsFromStartAndEnd(String possibleTriple) {
        possibleTriple = possibleTriple.trim();
        if (!possibleTriple.isEmpty()) {
            if (Character.compare(possibleTriple.charAt(0), '.') == 0) {
                possibleTriple = possibleTriple.substring(1).trim();
            }
            if (!possibleTriple.isEmpty() && Character.compare(possibleTriple.charAt(possibleTriple.length() - 1), '.') == 0) {
                possibleTriple = possibleTriple.substring(0, possibleTriple.length() - 1).trim();
            }
        }
        return possibleTriple;
    }

    /**
     * //     * Use reflection to create a Spotlight instance with a given URL.
     * //     * This is a workaround because the API from {@link Spotlight} is down.
     * //     *
     * //     * @param url The URL of the Spotlight instance.
     * //     * @return A {@link Spotlight} instance with a custom URL.
     * //
     */
    public static Spotlight createCustomSpotlightInstance(String url) {
        Class<?> clazz = Spotlight.class;
        Spotlight spotlight;
        try {
            spotlight = (Spotlight) clazz.newInstance();
            Field requestURLField = spotlight.getClass().getDeclaredField("requestURL");
            requestURLField.setAccessible(true);
            requestURLField.set(spotlight, url);
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            spotlight = new Spotlight();
            log.error("Unable to change the Spotlight API URL using reflection. Using it's default value.", e);
        }
        return spotlight;
    }
}
