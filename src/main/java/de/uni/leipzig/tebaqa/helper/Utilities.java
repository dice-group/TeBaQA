package de.uni.leipzig.tebaqa.helper;

import joptsimple.internal.Strings;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    public static Pattern BETWEEN_CIRCUMFLEX = Pattern.compile("\\^(.*?)\\^");
    public static Pattern SPARQL_VARIABLE = Pattern.compile("\\?\\w+");
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

    /**
     * Resolves all namespaces in a sparql query.
     *
     * @param query The query with namespaces
     * @return The given query where all namespaces are replaced with their full URI.
     */
    static String resolveNamespaces(String query) {
        Query q = QueryFactory.create(query);
        Map<String, String> nsPrefixMap = q.getPrefixMapping().getNsPrefixMap();
        String queryLowerCase = query.toLowerCase();
        int startOfQuery = 0;
        if (queryLowerCase.contains("select")) {
            startOfQuery = queryLowerCase.indexOf("select");
        } else if (queryLowerCase.contains("ask")) {
            startOfQuery = queryLowerCase.indexOf("ask");
        } else {
            log.error("Unable to determine query type of query: " + query);
        }
        final String[] queryWithoutPrefix = {query.substring(startOfQuery, query.length())};
        nsPrefixMap.forEach((s, s2) -> queryWithoutPrefix[0] = queryWithoutPrefix[0].replace(s + ":", "<" + s2));
        int startPosition = -1;
        int endPosition = -1;
        for (int i = 0; i < queryWithoutPrefix[0].length(); i++) {
            if (queryWithoutPrefix[0].charAt(i) == '<' && i + 1 < queryWithoutPrefix[0].length()
                    && (queryWithoutPrefix[0].charAt(i + 1) != '?' && queryWithoutPrefix[0].charAt(i + 1) != ' ')) {
                startPosition = i;
            } else if ((queryWithoutPrefix[0].charAt(i) == ' '
                    || queryWithoutPrefix[0].charAt(i) == ';')
                    && (i > 0 && queryWithoutPrefix[0].charAt(i - 1) != '>')) {
                endPosition = i;
            } else if (queryWithoutPrefix[0].charAt(i) == '.'
                    && i < queryWithoutPrefix[0].length()
                    && (queryWithoutPrefix[0].charAt(i + 1) == ' '
                    || queryWithoutPrefix[0].charAt(i + 1) == '\n'
                    || queryWithoutPrefix[0].charAt(i + 1) == '}')) {
                endPosition = i;
            } else if (i > 0 && queryWithoutPrefix[0].charAt(i - 1) == '>') {
                startPosition = -1;
            }
            if (startPosition > 0 && endPosition > 0 && startPosition < endPosition) {
                queryWithoutPrefix[0] = queryWithoutPrefix[0].replace(queryWithoutPrefix[0].substring(startPosition, endPosition),
                        queryWithoutPrefix[0].substring(startPosition, endPosition) + ">");
                startPosition = -1;
                endPosition = -1;
            }
        }

        String[] split = queryWithoutPrefix[0].replaceAll("\\s+", " ").split(" ");
        for (int i = 0; i < split.length; i++) {
            if (split[0].startsWith("http://") || split[0].startsWith("https://")) {
                split[0] = "<" + split[0] + ">";
            }
        }
        return String.join(" ", split).trim();
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
                        triples.add(removeDotsFromStartAndEnd(possibleTriple));
                    }
                }
            }
            //Add the last triple which may not end with a .
            triples.add(removeDotsFromStartAndEnd(group.substring(lastFoundTripleIndex, group.length())));
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
        //TODO At the moment every placeholder is filled with every class/property. Check which replacement fits (with the POS tags)
        for (String triple : triples) {
            Matcher m = ARGUMENTS_BETWEEN_SPACES.matcher(triple);
            int position = 0;
            while (m.find()) {
                String argument = m.group();
                if (argument.startsWith("<^")) {
                    if (position == 0 || position == 2) {
                        pattern = pattern.replace(argument, "?class_");
                    } else if (position == 1) {
                        pattern = pattern.replace(argument, "?property_");
                    } else {
                        log.error("Invalid position in triple:" + triple);
                    }
                }
                position++;
            }
        }

        String classValues = " VALUES (?class_) {(<" + Strings.join(classResources, ">) (<") + ">)}";
        String propertyValues = " VALUES (?property_) {(<" + Strings.join(propertyResources, ">) (<") + ">)}";

        return addToLastTriple(pattern, classValues + propertyValues);
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
}