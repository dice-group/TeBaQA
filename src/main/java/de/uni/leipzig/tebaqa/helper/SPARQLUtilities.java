package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

public class SPARQLUtilities {
    public static Pattern SPLIT_TRIPLE_PATTERN = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
    private static Logger log = Logger.getLogger(SPARQLUtilities.class);
    public static int QUERY_TYPE_UNKNOWN = -1;
    public static int ASK_QUERY = 0;
    public static int SELECT_QUERY = 1;


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

    public static SPARQLResultSet executeSPARQLQuery(String sparlQuery) {
        int resultType = SemanticAnalysisHelper.UNKNOWN_ANSWER_TYPE;
        List<String> result = new ArrayList<>();
        ParameterizedSparqlString qs = new ParameterizedSparqlString(sparlQuery);
        if (sparlQuery.contains("<^")) {
            log.error("ERROR: Invalid SPARQL Query: " + sparlQuery);
            return new SPARQLResultSet();
        } else {
            Query query;
            try {
                query = qs.asQuery();
            } catch (QueryParseException e) {
                log.error("QueryParseException: Unable to parse query: " + qs, e);
                return new SPARQLResultSet();
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
                    return new SPARQLResultSet();
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
                        return new SPARQLResultSet();
                    }
                    //log.info(String.join("; ", message));
                    //result.add(message);
                }
                if (result.size() > 1) {
                    resultType = SemanticAnalysisHelper.LIST_ANSWER_TYPE;
                } else if (result.size() == 1) {
                    String s = result.get(0);
                    if (s.endsWith("^^http://www.w3.org/2001/XMLSchema#integer") || s.endsWith("http://www.w3.org/2001/XMLSchema#positiveInteger")) {
                        resultType = SemanticAnalysisHelper.NUMBER_ANSWER_TYPE;
                    } else if (s.endsWith("^^http://www.w3.org/2001/XMLSchema#date")) {
                        resultType = SemanticAnalysisHelper.DATE_ANSWER_TYPE;
                    } else {
                        resultType = SemanticAnalysisHelper.SINGLE_RESOURCE_TYPE;
                    }
                } else {
                    resultType = SemanticAnalysisHelper.UNKNOWN_ANSWER_TYPE;
                }
            } else if (isAskType) {
                try {
                    boolean rs = qe.execAsk();
                    result.add(String.valueOf(rs));
                    resultType = SemanticAnalysisHelper.BOOLEAN_ANSWER_TYPE;
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
        return new SPARQLResultSet(result, resultType);
    }

    public static List<String> getDBpediaProperties() {
        Set<String> properties = new HashSet<>();
        String query = "select ?property where { ?property a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> } OFFSET %d LIMIT 10000";
        boolean gotResult = true;
        int offset = 0;
        while (gotResult) {
            String format = String.format(query, offset);
            List<String> result = SPARQLUtilities.executeSPARQLQuery(format).getResultSet();
            if (!result.isEmpty()) {
                properties.addAll(result);
                offset += 10000;
            } else {
                gotResult = false;
            }
        }
        return newArrayList(properties);
    }

    static boolean isDBpediaEntity(String s) {
        List<String> result = executeSPARQLQuery(String.format("ASK { VALUES (?r) {(<%s>)} {?r ?p ?o} UNION {?s ?r ?o} UNION {?s ?p ?r} }", s)).getResultSet();
        return result.size() == 1 && Boolean.valueOf(result.get(0));
    }

    /**
     * Resolves all namespaces in a sparql query.
     *
     * @param query The query with namespaces
     * @return The given query where all namespaces are replaced with their full URI.
     */
    public static String resolveNamespaces(String query) {
        Query q = QueryFactory.create(query);
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
        if (uri.startsWith("http://dbpedia.org/resource/")) {
            SPARQLResultSet sparqlResultSet = executeSPARQLQuery(String.format("PREFIX vrank:<http://purl.org/voc/vrank#> SELECT ?v FROM <http://people.aifb.kit.edu/ath/#DBpedia_PageRank> WHERE { <%s> vrank:hasRank/vrank:rankValue ?v. }", uri));
            List<String> resultSet = sparqlResultSet.getResultSet();
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
            SPARQLResultSet sparqlResultSet = executeSPARQLQuery(String.format("SELECT DISTINCT (COUNT(DISTINCT ?uri) as ?c) WHERE {  ?uri a <%s> . } ", uri));
            List<String> resultSet = sparqlResultSet.getResultSet();
            if (resultSet.size() == 1) {
                return Double.valueOf(resultSet.get(0).split("\\^")[0]);
            } else {
                return Double.MAX_VALUE;
            }
        } else {
            return Double.MAX_VALUE;
        }
    }
}
