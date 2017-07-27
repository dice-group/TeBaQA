package de.uni.leipzig.tebaqa.helper;

import org.aksw.qa.commons.datastructure.Entity;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.syntax.Element;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryMapping {
    private static Logger log = Logger.getLogger(QueryMapping.class);

    List<String> patternValues = new ArrayList<>();
    Map<String, String> mapping = new HashMap<>();
    String queryPattern = "";

    /**
     * Creates a Mapping between a part-of-speech tag sequence and a SPARQL Query.
     *
     * @param wordPosMap
     * @param sparqlQuery
     */
    QueryMapping(Map<String, String> wordPosMap, Query sparqlQuery) {
        Spotlight spotlight = Utilities.createCustomSpotlightInstance("http://model.dbpedia-spotlight.org/en/annotate");
        String posSequence = String.join(" ", wordPosMap.keySet());
        Map<String, List<Entity>> spotlightEntities = spotlight.getEntities(posSequence);
        Element queryPattern = sparqlQuery.getQueryPattern();
        this.queryPattern = sparqlQuery.toString();
        String queryPatternString = queryPattern.toString();

        for (int i = 0; i < spotlightEntities.values().size(); i++) {
            Entity spotlightEntity = spotlightEntities.get("en").get(i);
            List<Resource> resourceUris = spotlightEntity.getUris();
            for (Resource uri : resourceUris) {
                if (queryPatternString.contains(uri.toString())) {
                    //TODO Statt "foo" POS der URI einsetzen
                    this.queryPattern.replace(uri.toString(), "foo");
                }
            }
        }
    }

    /**
     * Creates a SPARQL Query Pattern like this: SELECT DISTINCT ?uri WHERE { ^NNP_0 ^VBZ_0 ?uri . }
     * Every entity which is recognized with the DBPedia Spotlight API is replaced by it's part-of-speech Tag.
     *
     * @return A string with part-of-speech tag placeholders.
     */
    public String getQueryPattern() {
        //TODO jede entity (zwischen <>) mit POS Tag und Nummer aus Frage ersetzen
        return queryPattern;
    }

    public List<String> getPatternValues() {
        return patternValues;
    }
}
