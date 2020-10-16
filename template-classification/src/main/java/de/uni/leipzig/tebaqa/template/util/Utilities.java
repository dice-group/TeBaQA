package de.uni.leipzig.tebaqa.template.util;

import de.uni.leipzig.tebaqa.template.model.Cluster;
import de.uni.leipzig.tebaqa.template.model.CustomQuestion;
import de.uni.leipzig.tebaqa.template.model.QueryTemplateMapping;
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

    /**
     * Extracts a map of possible query templates and their graph patterns.
     *
     * @param queryClusters The questions which contain a SPARQL query which will be used as template.
     * @return A list which contains SPARQL query templates, divided by their number of entities and classes and by
     * their query type (ASK or SELECT).
     */
    public static Map<String, QueryTemplateMapping> mapGraphToTemplates(List<Cluster> queryClusters) {
        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        //Set<String> wellKnownPredicates = Sets.union(commonTuples[0].keySet(), commonTuples[1].keySet());
        for (Cluster c : queryClusters) {
            String graph = c.getGraph();
            QueryTemplateMapping mapping = new QueryTemplateMapping();
            // if (c.size() > 10){
            for (CustomQuestion question : c.getQuestions()) {
                String query = question.getQuery();
                //QueryMappingFactoryLabels queryMappingFactory = new QueryMappingFactoryLabels(question.getQuestionText(), query,this);
                String queryPattern = SPARQLUtilities.resolveNamespaces(query);
                queryPattern = queryPattern.replace(" a ", " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ");
                int i = 0;
                String regex = "<(.+?)>";
                Pattern pattern = Pattern.compile(regex);
                Matcher m = pattern.matcher(queryPattern);
                HashMap<String, Integer> mappedUris = new HashMap<>();

                while (m.find()) {
                    String group = m.group();
                    if (!group.contains("^") && !group.contains("http://www.w3.org/2001/XMLSchema")) {
                        //if (!wellKnownPredicates.contains(m.group(1))) {
                        if (!mappedUris.containsKey(Pattern.quote(group)))
                            mappedUris.put(Pattern.quote(group), i);
                        queryPattern = queryPattern.replaceFirst(Pattern.quote(group), "res/" + mappedUris.get(Pattern.quote(group)));
                        i++;
                        //}
                    }
                }
                boolean isSuperlativeDesc = false;
                boolean isSuperlativeAsc = false;
                boolean isCountQuery = false;

                if (queryPattern.toLowerCase().contains("order by desc") && queryPattern.toLowerCase().contains("limit 1")) {
                    isSuperlativeDesc = true;
                } else if (queryPattern.toLowerCase().contains("order by asc") && queryPattern.toLowerCase().contains("limit 1")) {
                    isSuperlativeAsc = true;
                }
                if (queryPattern.toLowerCase().contains("count")) {
                    isCountQuery = true;
                }

                // TODO optimization: make a static set and use set intersection for conditions
                if (!queryPattern.toLowerCase().contains("http://dbpedia.org/resource/")
                        && !queryPattern.toLowerCase().contains("'")
                        && !queryPattern.toLowerCase().contains("union")
                        && !queryPattern.toLowerCase().contains("sum") && !queryPattern.toLowerCase().contains("avg")
                        && !queryPattern.toLowerCase().contains("min") && !queryPattern.toLowerCase().contains("max")
                        && !queryPattern.toLowerCase().contains("filter") && !queryPattern.toLowerCase().contains("bound")) {
                    int classCnt = 0;
                    int propertyCnt = 0;

                    List<String> triples = Utilities.extractTriples(queryPattern);
                    for (String triple : triples) {
                        Matcher argumentMatcher = Utilities.ARGUMENTS_BETWEEN_SPACES.matcher(triple);
                        int argumentCnt = 0;
                        while (argumentMatcher.find()) {
                            String argument = argumentMatcher.group();
                            //TODO at this point the matched argument is either a ?variable or res/n, both conditions do not match
                            if (argument.startsWith("<^") && (argumentCnt == 0 || argumentCnt == 2)) {
                                classCnt++;
                            } else if (argument.startsWith("<^") && argumentCnt == 1) {
                                propertyCnt++;
                            }
                            argumentCnt++;
                        }
                    }

                    // TODO revisit class and property count logic
                    int finalClassCnt = classCnt;
                    int finalPropertyCnt = propertyCnt;
                    if (!mapping.getNumberOfProperties().contains(finalPropertyCnt))
                        mapping.getNumberOfProperties().add(finalPropertyCnt);
                    if (!mapping.getNumberOfClasses().contains(finalClassCnt))
                        mapping.getNumberOfClasses().add(finalClassCnt);
                    int queryType = SPARQLUtilities.getQueryType(query);

                    if (isSuperlativeDesc) {
                        mapping.setSelectSuperlativeDescTemplate(queryPattern, question.getQuery());
                    } else if (isSuperlativeAsc) {
                        mapping.setSelectSuperlativeAscTemplate(queryPattern, question.getQuery());
                    } else if (isCountQuery) {
                        mapping.setCountTemplate(queryPattern, question.getQuery());
                    }

                    if (queryType == SPARQLUtilities.SELECT_QUERY) {
                        mapping.setSelectTemplate(queryPattern, question.getQuery());
                    } else if (queryType == SPARQLUtilities.ASK_QUERY) {
                        mapping.setAskTemplate(queryPattern, question.getQuery());
                    }

                    //add graph <-> query template mapping entry
                    mappings.put(graph, mapping);

                    //log.info(queryPattern);
                }
            }
            //}
        }
        return mappings;
    }
}
