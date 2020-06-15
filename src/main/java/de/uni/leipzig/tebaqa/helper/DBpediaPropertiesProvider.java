package de.uni.leipzig.tebaqa.helper;


import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;

import java.util.*;

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
        //String query = "select ?property where { ?property a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> } OFFSET %d LIMIT 10000";
        String query = "select ?property where { ?s ?property ?o } OFFSET %d LIMIT 10000";
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
    public static HashMap<Resource,String> getDBpediaPropertiesAndLabels() {
        HashMap<Resource,String> properties = new HashMap<>();
        String query = "SELECT ?property ?label WHERE { ?property a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property>. ?property <http://www.w3.org/2000/01/rdf-schema#label> ?label. FILTER (LANG(?label) = \"en\")} OFFSET %d LIMIT 10000";
        boolean gotResult = true;
        int offset = 0;
        while (gotResult) {
            String format = String.format(query, offset);
            QueryExecution qe = QueryExecutionFactory.sparqlService("https://dbpedia.org/sparql",format);
            ResultSet rs = ResultSetFactory.copyResults(qe.execSelect());
            if (rs.hasNext()) {
                offset += 10000;
                rs.forEachRemaining(s -> properties.put(s.get("property").asResource(),s.get("label").toString()) );
            } else {
                gotResult = false;
            }


        }
        return properties;
    }
}
