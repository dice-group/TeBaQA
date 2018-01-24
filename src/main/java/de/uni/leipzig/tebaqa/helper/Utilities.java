package de.uni.leipzig.tebaqa.helper;

import joptsimple.internal.Strings;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utilities {

    public static Pattern BETWEEN_CIRCUMFLEX = Pattern.compile("\\^(.*?)\\^");
    public static Pattern SPARQL_VARIABLE = Pattern.compile("\\?\\w+");
    public static Pattern BETWEEN_LACE_BRACES = Pattern.compile("<(.*?)>");
    public static Pattern BETWEEN_CURLY_BRACES = Pattern.compile("\\{(.*?)\\}");
    public static Pattern ARGUMENTS_BETWEEN_SPACES = Pattern.compile("\\S+");

    private static Logger log = Logger.getLogger(Utilities.class);

    /**
     * Use reflection to create a Spotlight instance with a given URL.
     * This is a workaround because the API from {@link Spotlight} is down.
     *
     * @param url The URL of the Spotlight instance.
     * @return A {@link Spotlight} instance with a custom URL.
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
                        if (!t.isEmpty()) {
                            triples.add(t);
                        }
                    }
                }
            }
            //Add the last triple which may not end with a .
            String t = removeDotsFromStartAndEnd(group.substring(lastFoundTripleIndex, group.length()));
            if (!t.isEmpty()) {
                triples.add(t);
            }
        }

        return triples;
    }

    private static String removeDotsFromStartAndEnd(String possibleTriple) {
        possibleTriple = possibleTriple.trim();
        if (!possibleTriple.isEmpty()) {
            if (Character.compare(possibleTriple.charAt(0), '.') == 0) {
                possibleTriple = possibleTriple.substring(1, possibleTriple.length()).trim();
            }
            if (!possibleTriple.isEmpty() && Character.compare(possibleTriple.charAt(possibleTriple.length() - 1), '.') == 0) {
                possibleTriple = possibleTriple.substring(0, possibleTriple.length() - 1).trim();
            }
        }
        return possibleTriple;
    }

    public static boolean isEven(int x) {
        return (x % 2) == 0;
    }

    public static String fillPattern(String pattern, List<String> classResources, List<String> propertyResources) {
        List<String> triples = extractTriples(pattern);
        List<String> triplesWithoutFilters = triples.parallelStream()
                .filter(s -> !s.toLowerCase().contains("filter") && !s.toLowerCase().contains("optional"))
                .collect(Collectors.toList());
        //TODO At the moment every placeholder is filled with every class/property. Check which replacement fits (with the POS tags)
        int classReplacementCount = 0;
        int propertyReplacementCount = 0;
        Map<String, String> replacements = new HashMap<>();
        for (String triple : triplesWithoutFilters) {
            Matcher m = ARGUMENTS_BETWEEN_SPACES.matcher(triple);
            int position = 0;
            while (m.find()) {
                String argument = m.group();
                if (argument.startsWith("<^")) {
                    if (position == 0 || position == 2) {
                        String replacement = "?class_" + classReplacementCount;
                        pattern = pattern.replace(argument, replacement);
                        replacements.put(argument, replacement);
                        classReplacementCount++;
                    } else if (position == 1) {
                        String replacement = "?property_" + propertyReplacementCount;
                        pattern = pattern.replace(argument, replacement);
                        replacements.put(argument, replacement);
                        propertyReplacementCount++;
                    } else {
                        log.error("Invalid position in triple:" + triple);
                    }
                }
                position++;
            }
        }
        StringBuilder classValues = new StringBuilder();
        if (classReplacementCount > 0) {
            for (int i = 0; i < triplesWithoutFilters.size(); i++) {
                classValues.append(String.format(" VALUES (?class_%d) {(<", i)).append(Strings.join(classResources, ">) (<")).append(">)}");
            }
        }

        StringBuilder propertyValues = new StringBuilder();
        if (propertyReplacementCount > 0) {
            for (int i = 0; i < triplesWithoutFilters.size(); i++) {
                propertyValues.append(String.format(" VALUES (?property_%d) {(<", i)).append(Strings.join(propertyResources, ">) (<")).append(">)}");
            }
        }



        String filterClauses = SPARQLUtilities.createFilterClauses(triplesWithoutFilters, replacements);

        return addToLastTriple(pattern, classValues.append(propertyValues.toString()).append(filterClauses).toString());
    }

    private static String addToLastTriple(String pattern, String s) {
        Matcher m = BETWEEN_CURLY_BRACES.matcher(pattern);
        String lastTriple = "";
        String newLastTriple = "";
        while (m.find()) {
            lastTriple = m.group(m.groupCount());
            newLastTriple = lastTriple;
            if (!newLastTriple.trim().endsWith(".")) {
                newLastTriple = newLastTriple + ".";
            }
            newLastTriple = newLastTriple + s;
        }
        String result = pattern.replace(lastTriple, newLastTriple);
        if (result.isEmpty()) {
            log.error("Unable to put together SPARQL Query: " + pattern + "; And: " + s);
        }
        return result;
    }

    public static <E> List<List<E>> generatePermutations(List<E> original) {
        if (original.size() == 0) {
            List<List<E>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        E firstElement = original.remove(0);
        List<List<E>> returnValue = new ArrayList<>();
        List<List<E>> permutations = generatePermutations(original);
        for (List<E> smallerPermutated : permutations) {
            for (int index = 0; index <= smallerPermutated.size(); index++) {
                List<E> temp = new ArrayList<>(smallerPermutated);
                temp.add(index, firstElement);
                returnValue.add(temp);
            }
        }
        return returnValue;
    }

    private static boolean isProperty(String rdfResource) {
        return Character.isLowerCase(rdfResource.substring(rdfResource.lastIndexOf('/') + 1).charAt(0));
    }

    private static boolean isClass(String rdfResource) {
        return Character.isUpperCase(rdfResource.substring(rdfResource.lastIndexOf('/') + 1).charAt(0));
    }

    static double getLevenshteinRatio(String s, String s2) {
        int lfd = StringUtils.getLevenshteinDistance(s2, s);
        return ((double) lfd) / (Math.max(s2.length(), s.length()));
    }
}
