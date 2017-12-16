package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.model.SPARQLResultSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

public class DBpediaPropertiesProvider {
    private static List<String> properties = new ArrayList<>();

    //do not instantiate
    private DBpediaPropertiesProvider() {
    }

    public static List<String> getDBpediaProperties() {
        if (properties.isEmpty()) {
            properties = requestDBpediaProperties();
        }
        return properties;
    }

    private static List<String> requestDBpediaProperties() {
        Set<String> properties = new HashSet<>();
        String query = "select ?property where { ?property a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> } OFFSET %d LIMIT 10000";
        boolean gotResult = true;
        int offset = 0;
        while (gotResult) {
            String format = String.format(query, offset);
            List<String> result = new ArrayList<>();
            List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(format);
            sparqlResultSets.forEach( sparqlResultSet -> result.addAll(sparqlResultSet.getResultSet()));
            if (!result.isEmpty()) {
                properties.addAll(result);
                offset += 10000;
            } else {
                gotResult = false;
            }
        }
        return newArrayList(properties);
    }
}
