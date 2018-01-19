package de.uni.leipzig.tebaqa.helper;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SPARQLUtilities {
    public final static String LABEL_SPARQL = "SELECT ?label  WHERE { <%1$s> <http://www.w3.org/2000/01/rdf-schema#label> ?label . FILTER(lang(?label) = \"en\")}";
    public final static String IMAGE_SPARQL = "SELECT ?thumbnail WHERE { <%1$s> <http://dbpedia.org/ontology/thumbnail> ?thumbnail . }";
    public final static String WIKI_LINK_SPARQL = "SELECT ?primaryTopic WHERE { <%1$s> <http://xmlns.com/foaf/0.1/isPrimaryTopicOf> ?primaryTopic  . }";
    public final static String DESCRIPTION_SPARQL = "SELECT ?description WHERE { <%1$s> <http://purl.org/dc/terms/description> ?description . FILTER(lang(?description)=\"en\") }";
    public final static String ABSTRACT_SPARQL = "SELECT ?abstract WHERE { <%1$s> <http://dbpedia.org/ontology/abstract> ?abstract .  FILTER(lang(?abstract)=\"en\")  }";
    private final static String GET_REDIRECTS_SPARQL = "SELECT ?redirectsTo WHERE { <%1$s> <http://dbpedia.org/ontology/wikiPageRedirects> ?redirectsTo }";
    private static Pattern SPLIT_TRIPLE_PATTERN = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
    private static Logger log = Logger.getLogger(SPARQLUtilities.class);
    public static int QUERY_TYPE_UNKNOWN = -1;
    public static int ASK_QUERY = 1;
    public static int SELECT_QUERY = 2;
    public static int SELECT_SUPERLATIVE_ASC_QUERY = 3;
    public static int SELECT_SUPERLATIVE_DESC_QUERY = 4;
    public static int SELECT_COUNT_QUERY = 5;


    /**
     * Determines the type of a SPARQL query.
     *
     * @param q The SPARQL query.
     * @return 0 for a ASK query, 1 for a SELECT query and -1 for everything else.
     */
    public static int getQueryType(String q) {
        Query query = QueryFactory.create(q);
        if (query.isAskType()) {
            return ASK_QUERY;
        } else if (query.isSelectType()) {
            return SELECT_QUERY;
        } else {
            return QUERY_TYPE_UNKNOWN;
        }
    }

    public static List<SPARQLResultSet> executeSPARQLQuery(String sparlQuery) {
        List<SPARQLResultSet> results = new ArrayList<>();
        int resultType;
        List<String> result = new ArrayList<>();
        ParameterizedSparqlString qs = new ParameterizedSparqlString(sparlQuery);
        if (sparlQuery.contains("<^")) {
            log.error("ERROR: Invalid SPARQL Query: " + sparlQuery);
            return results;
        } else {
            Query query;
            try {
                query = qs.asQuery();
            } catch (QueryParseException e) {
                log.error("QueryParseException: Unable to parse query: " + qs, e);
                return results;
            }
            QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
            qe.setTimeout(20000, 20000);
            boolean isAskType = query.isAskType();
            boolean isSelectType = query.isSelectType();

            if (isSelectType) {
                ResultSet rs;
                try {
                    rs = qe.execSelect();
                } catch (QueryExceptionHTTP e) {
                    log.error("HTTP Exception while executing SPARQL query: " + sparlQuery, e);
                    return results;
                }
                while (rs.hasNext()) {
                    QuerySolution s = rs.nextSolution();
                    //String message;
                    try {
                        Iterator<String> varNames = s.varNames();
                        for (Iterator<String> it = varNames; it.hasNext(); ) {
                            String varName = it.next();
                            result.add(s.get(varName).toString());
                        }
                        //message = String.valueOf(s.getResource(resultVariableName));
                    } catch (ClassCastException e) {
                        log.error("Unable to parse response! SPARQL: " + sparlQuery, e);
                        return results;
                    }
                    //log.info(String.join("; ", message));
                    //result.add(message);
                }
                if (result.size() > 1) {
                    boolean listIsMixed = result.stream().anyMatch(s -> !isResource(s));
                    if (listIsMixed) {
                        Set<String> dates = result.stream().filter(SPARQLUtilities::isDateFromXMLSchema).collect(Collectors.toSet());
                        if (dates.size() > 0) {
                            SPARQLResultSet dateResult = new SPARQLResultSet(Lists.newArrayList(dates), SPARQLResultSet.DATE_ANSWER_TYPE);
                            results.add(dateResult);
                        }

                        Set<String> numbers = result.stream().filter(SPARQLUtilities::isNumberFromXMLSchema).collect(Collectors.toSet());
                        if (numbers.size() > 0) {
                            SPARQLResultSet numberResult = new SPARQLResultSet(Lists.newArrayList(numbers), SPARQLResultSet.NUMBER_ANSWER_TYPE);
                            results.add(numberResult);
                        }

                        Set<String> resources = result.stream().filter(SPARQLUtilities::isResource).collect(Collectors.toSet());
                        if (resources.size() > 0) {
                            SPARQLResultSet resourceResult;
                            if (resources.size() > 1) {
                                resourceResult = new SPARQLResultSet(Lists.newArrayList(resources), SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE);
                            } else {
                                resourceResult = new SPARQLResultSet(Lists.newArrayList(resources), SPARQLResultSet.SINGLE_RESOURCE_TYPE);
                            }
                            results.add(resourceResult);
                        }

                        Set<String> strings = result.stream().filter(s -> !isResource(s) && !isDateFromXMLSchema(s) && !isResource(s) || isStringFromXMLSchema(s)).collect(Collectors.toSet());
                        if (strings.size() > 0) {
                            SPARQLResultSet stringResult = new SPARQLResultSet(Lists.newArrayList(strings), SPARQLResultSet.STRING_ANSWER_TYPE);
                            results.add(stringResult);
                        }
                    } else {
                        resultType = SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE;
                        results.add(new SPARQLResultSet(result, resultType));
                    }
                } else if (result.size() == 1) {
                    String s = result.get(0);
                    //Check for scientific numbers like 3.40841e+10
                    if (isNumericOrScientific(s)) {
                        resultType = SPARQLResultSet.NUMBER_ANSWER_TYPE;
                    } else if (isNumberFromXMLSchema(s)) {
                        resultType = SPARQLResultSet.NUMBER_ANSWER_TYPE;
                    } else if (isDateFromXMLSchema(s)) {
                        resultType = SPARQLResultSet.DATE_ANSWER_TYPE;
                    } else if (isResource(s)) {
                        resultType = SPARQLResultSet.SINGLE_RESOURCE_TYPE;
                    } else {
                        resultType = SPARQLResultSet.STRING_ANSWER_TYPE;
                    }
                    results.add(new SPARQLResultSet(result, resultType));
                } else {
                    resultType = SPARQLResultSet.UNKNOWN_ANSWER_TYPE;
                    results.add(new SPARQLResultSet(result, resultType));
                }
            } else if (isAskType) {
                try {
                    boolean rs = qe.execAsk();
                    result.add(String.valueOf(rs));
                    resultType = SPARQLResultSet.BOOLEAN_ANSWER_TYPE;
                    results.add(new SPARQLResultSet(result, resultType));
                } catch (Exception e) {
                    log.error("HTTP Exception while creating query: " + sparlQuery, e);
                    //throw e;
                }
            } else {
                log.error("Unknown query type: " + sparlQuery);
            }
        }

        //if (!result.isEmpty()) {
        //log.info("Result: " + Strings.join(result, "; "));
        //}
        return results;
    }

    private static boolean isResource(String s) {
        return s.startsWith("http://dbpedia.org/resource/");
    }

    private static boolean isNumericOrScientific(String s) {
        return StringUtils.isNumeric(s) || (Character.isDigit(s.charAt(0))
                && Character.isDigit(s.charAt(s.length() - 1)) && (s.toLowerCase().contains("e+") || s.toLowerCase().contains("e-")));
    }

    private static boolean isDateFromXMLSchema(String s) {
        return s.endsWith("^^http://www.w3.org/2001/XMLSchema#date") || s.endsWith("^^http://www.w3.org/2001/XMLSchema#gYear");
    }

    private static boolean isStringFromXMLSchema(String s) {
        return s.endsWith("^^http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
    }

    private static boolean isNumberFromXMLSchema(String s) {
        return s.endsWith("^^http://www.w3.org/2001/XMLSchema#integer")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#positiveInteger")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#float")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#decimal")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#nonPositiveInteger")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#negativeInteger")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#int")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#short")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#long")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#double");
    }

    static boolean isDBpediaEntity(String s) {
        if (s.contains("^^")) {
            return false;
        } else {
            List<SPARQLResultSet> sparqlResultSets = executeSPARQLQuery(String.format("ASK { VALUES (?r) {(<%s>)} {?r ?p ?o} UNION {?s ?r ?o} UNION {?s ?p ?r} }", s));
            if (sparqlResultSets.size() == 1) {
                List<String> result = sparqlResultSets.get(0).getResultSet();
                return result.size() == 1 && Boolean.valueOf(result.get(0));
            } else {
                return false;
            }
        }
    }

    /**
     * Resolves all namespaces in a sparql query.
     *
     * @param query The query with namespaces
     * @return The given query where all namespaces are replaced with their full URI.
     */
    public static String resolveNamespaces(String query) {
        if (!query.toLowerCase().contains("prefix")) {
            return query;
        } else {
            Query q;
            try {
                q = QueryFactory.create(query);
            } catch (QueryException e) {
                log.error("QueryException: Unable to parse query to remove : " + query);
                return "";
            }
            Map<String, String> nsPrefixMap = q.getPrefixMapping().getNsPrefixMap();
            if (nsPrefixMap.isEmpty()) {
                return query.trim();
            } else {
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
        }
    }

    static String createFilterClauses(List<String> triples, Map<String, String> replacements) {
        StringBuilder result = new StringBuilder();
        List<List<String>> triplesSplitted = new ArrayList<>();
        triples.forEach(s -> {
                    Matcher matcher = SPLIT_TRIPLE_PATTERN.matcher(s);
                    List<String> currentTriple = new ArrayList<>();
                    while (matcher.find()) {
                        String group = matcher.group();
                        if (group.startsWith("@")) {
                            String element = currentTriple.get(currentTriple.size() - 1) + group;
                            currentTriple.set(currentTriple.size() - 1, element);
                        } else {
                            if (!group.toLowerCase().startsWith("?") && !group.toLowerCase().startsWith("<")
                                    && !group.toLowerCase().startsWith("'") && !group.toLowerCase().startsWith("\"")) {
                                group = "'" + group + "'";
                            }
                            currentTriple.add(replacements.getOrDefault(group, group));
                        }
                    }
                    triplesSplitted.add(currentTriple);
                }
        );

        List<Map<List<String>, List<String>>> filterClauses = new ArrayList<>();
        triplesSplitted.forEach(currentTriple -> triplesSplitted.forEach(otherTriple -> {
            if (!currentTriple.equals(otherTriple)) {
                Map<List<String>, List<String>> filterClause = new HashMap<>();
                filterClause.put(currentTriple, otherTriple);
                filterClauses.add(filterClause);
            }
        }));

        filterClauses.forEach((filterMap) -> {
            Optional<Entry<List<String>, List<String>>> any = filterMap.entrySet().stream().findAny();
            if (any.isPresent()) {
                Entry<List<String>, List<String>> filterMapping = any.get();
                List<String> triple1 = filterMapping.getKey();
                List<String> triple2 = filterMapping.getValue();

                if (triple1.size() == 3 && triple2.size() == 3) {
                    result.append(String.format(" FILTER (CONCAT( %s, %s, %s ) != CONCAT( %s, %s, %s )) ",
                            triple1.get(0), triple1.get(1), triple1.get(2), triple2.get(0), triple2.get(1), triple2.get(2)));
                } else {
                    log.error(String.format("ERROR: Unable to generate FILTER statements because the triples don't have " +
                            "exactly 3 parts! current triple: {%s}; next triple: {%s}", String.join(" ", triple1), String.join(" ", triple2)));
                }
            }
        });
        return result.toString();

    }

    /**
     * Fetches the page rank for a given resource. If the resource isn't valid (e.g. if it's a ontology instead), {@link Double}.MAX_VALUE is returned instead.
     *
     * @param uri The resource which page rank is returned.
     * @return The page rank of the resource.
     */
    public static Double getPageRank(String uri) {
        if (isResource(uri)) {
            List<SPARQLResultSet> sparqlResultSets = executeSPARQLQuery(String.format("PREFIX vrank:<http://purl.org/voc/vrank#> SELECT ?v FROM <http://people.aifb.kit.edu/ath/#DBpedia_PageRank> WHERE { <%s> vrank:hasRank/vrank:rankValue ?v. }", uri));
            List<String> resultSet = new ArrayList<>();
            sparqlResultSets.forEach(sparqlResultSet -> resultSet.addAll(sparqlResultSet.getResultSet()));
            if (resultSet.size() == 1) {
                return Double.valueOf(resultSet.get(0).split("\\^")[0]);
            } else {
                return Double.MAX_VALUE;
            }
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static Double countOntologyUsage(String uri) {
        String[] split = uri.split("/");
        String entity = split[split.length - 1];
        if (uri.startsWith("http://dbpedia.org/ontology/") && Character.isUpperCase(entity.charAt(0))) {
            List<SPARQLResultSet> sparqlResultSets = executeSPARQLQuery(String.format("SELECT DISTINCT (COUNT(DISTINCT ?uri) as ?c) WHERE {  ?uri a <%s> . } ", uri));
            List<String> resultSet = new ArrayList<>();
            sparqlResultSets.forEach(sparqlResultSet -> resultSet.addAll(sparqlResultSet.getResultSet()));
            if (resultSet.size() == 1) {
                return Double.valueOf(resultSet.get(0).split("\\^")[0]);
            } else {
                return Double.MAX_VALUE;
            }
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static String getRedirect(String resource) {
        List<SPARQLResultSet> sparqlResultSets = executeSPARQLQuery(String.format(GET_REDIRECTS_SPARQL, resource));
        if (sparqlResultSets.size() >= 1) {
            List<String> resultSet = sparqlResultSets.get(0).getResultSet();
            if (resultSet.size() > 0) {
                //There should be only one redirection
                return resultSet.get(0);
            }
        }
        return "";
    }
}
