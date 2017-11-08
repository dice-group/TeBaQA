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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

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
                log.error("Invalid SPARQL Query: " + sparlQuery);
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
}
