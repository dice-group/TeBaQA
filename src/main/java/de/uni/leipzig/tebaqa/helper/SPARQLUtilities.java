package de.uni.leipzig.tebaqa.helper;

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
import org.assertj.core.util.Lists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import static com.google.common.collect.Lists.newArrayList;

public class SPARQLUtilities {
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

    public static List<String> executeSPARQLQuery(String sparlQuery) {
        List<String> result = new ArrayList<>();
        Matcher m = Utilities.SPARQL_VARIABLE.matcher(sparlQuery);
        if (m.find()) {
            ParameterizedSparqlString qs = new ParameterizedSparqlString(sparlQuery);
            if (sparlQuery.contains("<^")) {
                log.error("ERROR: Invalid SPARQL Query: " + sparlQuery);
                return Lists.emptyList();
            } else {
                Query query;
                try {
                    query = qs.asQuery();
                } catch (QueryParseException e) {
                    log.error("QueryParseException: Unable to parse query: " + qs, e);
                    return Lists.emptyList();
                }
                QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
                qe.setTimeout(5000);
                boolean isAskType = query.isAskType();
                boolean isSelectType = query.isSelectType();

                if (isSelectType) {
                    ResultSet rs;
                    try {
                        rs = qe.execSelect();
                    } catch (QueryExceptionHTTP e) {
                        log.error("HTTP Exception while executing SPARQL query: " + sparlQuery, e);
                        return Lists.emptyList();
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
                            return Lists.emptyList();
                        }
                        //log.info(String.join("; ", message));
                        //result.add(message);
                    }
                } else if (isAskType) {
                    try {
                        boolean rs = qe.execAsk();
                        result.add(String.valueOf(rs));
                    } catch (Exception e) {
                        log.error("HTTP Exception while creating query: " + sparlQuery, e);
                        //throw e;
                    }
                } else {
                    log.error("Unknown query type: " + sparlQuery);
                }
            }

        }
        if (!result.isEmpty()) {
            //log.info("Result: " + Strings.join(result, "; "));
        }
        return result;
    }

    public static List<String> getDBpediaProperties() {
        Set<String> properties = new HashSet<>();
        String query = "select ?property where { ?property a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> } OFFSET %d LIMIT 10000";
        boolean gotResult = true;
        int offset = 0;
        while (gotResult) {
            String format = String.format(query, offset);
            List<String> result = SPARQLUtilities.executeSPARQLQuery(format);
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
        List<String> result = executeSPARQLQuery(String.format("ASK { VALUES (?r) {(<%s>)} {?r ?p ?o} UNION {?s ?r ?o} UNION {?s ?p ?r} }", s));
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
}
