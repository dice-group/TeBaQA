package de.uni.leipzig.tebaqa.template.util;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.shared.PrefixMapping;

import java.util.*;

// TODO methods are in common, change references
public class TextUtils {

    public final static String NON_WORD_CHARACTERS_REGEX = "[^a-zA-Z0-9_äÄöÖüÜ']";
    private static Map<String, String> WIKIDATA_PREFIXES;


    public static String trim(String s) {
        return s.trim().replaceAll("\n", " ").replaceAll("\\s{2,}", " ").trim();
    }

    public static int countWords(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        List<String> words = new ArrayList<>(Arrays.asList(s.split(NON_WORD_CHARACTERS_REGEX)));
        words.removeIf(String::isEmpty);
        return words.size();
    }

    public static String cleanQuery(String queryString) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(getWikiDataPrefixes());
        pss.append(queryString);
        Query query = pss.asQuery();

        return query.toString().trim()
                //replace newlines with space
                .replaceAll("\n", " ")
                //replace every variable with ?
                .replaceAll("\\?[a-zA-Z\\d]+", " ? ")
                //replace every number(e.g. 2 or 2.5) with a ?
                .replaceAll("\\s+\\d+\\.?\\d*", " ? ")
                //replace everything in quotes with ?
                .replaceAll("([\"'])(?:(?=(\\\\?))\\2.)*?\\1", " ? ")
                //remove everything between <>
                .replaceAll("<\\S*>", " <> ")
                //remove all SELECT, ASK, DISTINCT and WHERE keywords
                .replaceAll("(?i)(select|ask|where|distinct)", " ")
                //remove every consecutive spaces
                .replaceAll("\\s+", " ");
    }
    
    public static Map<String, String> getWikiDataPrefixes() {
        if(WIKIDATA_PREFIXES == null) {
            WIKIDATA_PREFIXES = new HashMap<>();

            WIKIDATA_PREFIXES.put("bd", "http://www.bigdata.com/rdf#");
            WIKIDATA_PREFIXES.put("cc", "http://creativecommons.org/ns#");
            WIKIDATA_PREFIXES.put("dct", "http://purl.org/dc/terms/");
            WIKIDATA_PREFIXES.put("geo", "http://www.opengis.net/ont/geosparql#");
            WIKIDATA_PREFIXES.put("ontolex", "http://www.w3.org/ns/lemon/ontolex#");
            WIKIDATA_PREFIXES.put("owl", "http://www.w3.org/2002/07/owl#");
            WIKIDATA_PREFIXES.put("p", "http://www.wikidata.org/prop/");
            WIKIDATA_PREFIXES.put("pq", "http://www.wikidata.org/prop/qualifier/");
            WIKIDATA_PREFIXES.put("pqn", "http://www.wikidata.org/prop/qualifier/value-normalized/");
            WIKIDATA_PREFIXES.put("pqv", "http://www.wikidata.org/prop/qualifier/value/");
            WIKIDATA_PREFIXES.put("pr", "http://www.wikidata.org/prop/reference/");
            WIKIDATA_PREFIXES.put("prn", "http://www.wikidata.org/prop/reference/value-normalized/");
            WIKIDATA_PREFIXES.put("prov", "http://www.w3.org/ns/prov#");
            WIKIDATA_PREFIXES.put("prv", "http://www.wikidata.org/prop/reference/value/");
            WIKIDATA_PREFIXES.put("ps", "http://www.wikidata.org/prop/statement/");
            WIKIDATA_PREFIXES.put("psn", "http://www.wikidata.org/prop/statement/value-normalized/");
            WIKIDATA_PREFIXES.put("psv", "http://www.wikidata.org/prop/statement/value/");
            WIKIDATA_PREFIXES.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            WIKIDATA_PREFIXES.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            WIKIDATA_PREFIXES.put("schema", "http://schema.org/");
            WIKIDATA_PREFIXES.put("skos", "http://www.w3.org/2004/02/skos/core#");
            WIKIDATA_PREFIXES.put("wd", "http://www.wikidata.org/entity/");
            WIKIDATA_PREFIXES.put("wdata", "http://www.wikidata.org/wiki/Special:EntityData/");
            WIKIDATA_PREFIXES.put("wdno", "http://www.wikidata.org/prop/novalue/");
            WIKIDATA_PREFIXES.put("wdref", "http://www.wikidata.org/reference/");
            WIKIDATA_PREFIXES.put("wds", "http://www.wikidata.org/entity/statement/");
            WIKIDATA_PREFIXES.put("wdt", "http://www.wikidata.org/prop/direct/");
            WIKIDATA_PREFIXES.put("wdtn", "http://www.wikidata.org/prop/direct-normalized/");
            WIKIDATA_PREFIXES.put("wdv", "http://www.wikidata.org/value/");
            WIKIDATA_PREFIXES.put("wikibase", "http://wikiba.se/ontology#");
            WIKIDATA_PREFIXES.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        }
        return WIKIDATA_PREFIXES;
    }
}
