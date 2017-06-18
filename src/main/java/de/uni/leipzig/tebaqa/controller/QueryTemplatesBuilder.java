package de.uni.leipzig.tebaqa.controller;

import de.uni.leipzig.tebaqa.model.QueryTemplate;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The QueryTemplatesBuilder creates clean templates with placeholders and extracts query modifier.
 */
class QueryTemplatesBuilder {
    private List<QueryTemplate> queryTemplates = new ArrayList<>();
    private static Logger log = Logger.getLogger(QueryTemplatesBuilder.class);

    QueryTemplatesBuilder(List<String> queries) {
        List<Query> askQueries = new ArrayList<>();
        List<Query> selectQueries = new ArrayList<>();

        for (String queryString : queries) {
            try {
                Query query = QueryFactory.create(queryString);
                if (query.isAskType()) {
                    askQueries.add(query);
                } else if (query.isSelectType()) {
                    selectQueries.add(query);
                } else {
                    log.warn("Query is neither from type ask nor type select: " + query);
                }
            } catch (QueryParseException e) {
                log.warn("Unable to parse query: " + queryString);
                throw e;
            }
        }
        List<String> askQueriesCleaned = cleanQueries(askQueries);
        List<String> selectQueriesCleaned = cleanQueries(selectQueries);

        getModifier(askQueriesCleaned);
        getModifier(selectQueriesCleaned);
    }

    private void getModifier(List<String> queries) {
        for (String query : queries) {
            queryTemplates.add(new QueryTemplate(query));
        }
    }

    private List<String> cleanQueries(List<Query> queries) {
        List<String> cleanedQueries = new ArrayList<>();
        for (Query query : queries) {
            query.setPrefixMapping(null);
            String trimmed = query.toString().trim()
                    .replaceAll("\n", " ")
                    //replace every variable with ?
                    .replaceAll("\\?[a-zA-Z\\d]+", " ? ")
                    //replace every number(e.g. 2 or 2.5) with a ?
                    .replaceAll("\\s+\\d+.?\\d*", " ? ")
                    //replace everything in quotes with ?
                    .replaceAll("([\"'])(?:(?=(\\\\?))\\2.)*?\\1", " ? ")
                    //remove everything between <>
                    .replaceAll("<\\S*>", " <> ")
                    //remove every consecutive spaces
                    .replaceAll("\\s+", " ")
                    //remove all SELECT, ASK, DISTINCT and WHERE keywords
                    .replaceAll("(?i)(select|ask|where|distinct)", " ");
            cleanedQueries.add(trimmed);
        }
        return cleanedQueries;
    }

    List<QueryTemplate> getQueryTemplates() {
        return queryTemplates;
    }
}
