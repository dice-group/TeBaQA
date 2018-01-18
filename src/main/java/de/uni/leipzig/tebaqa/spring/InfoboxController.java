package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.helper.SPARQLUtilities;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static de.uni.leipzig.tebaqa.spring.ExtendedQALDAnswer.extractAnswerString;

@RestController
public class InfoboxController {

    private static Logger log = Logger.getLogger(InfoboxController.class.getName());
    private final static String BASIC_INFO_SPARQL = "SELECT ?label ?abstract ?primaryTopic ?thumbnail ?description WHERE { " +
            "<%1$s>  <http://www.w3.org/2000/01/rdf-schema#label> ?label . " +
            "<%1$s> <http://xmlns.com/foaf/0.1/isPrimaryTopicOf> ?primaryTopic .  " +
            "OPTIONAL {    <%1$s> <http://dbpedia.org/ontology/thumbnail> ?thumbnail . } " +
            "OPTIONAL { <%1$s> <http://dbpedia.org/ontology/abstract> ?abstract .  " +
            " FILTER(lang(?abstract)=\"en\") } OPTIONAL {<%1$s> <http://purl.org/dc/terms/description> ?description . " +
            " FILTER(lang(?description)=\"en\") }  FILTER(lang(?label) = \"en\") }";
    private final static String LABEL_SPARQL = "SELECT ?label  WHERE { <%1$s> <http://www.w3.org/2000/01/rdf-schema#label> ?label . FILTER(lang(?label) = \"en\")}";
    private final static String IMAGE_SPARQL = "SELECT ?thumbnail WHERE { <%1$s> <http://dbpedia.org/ontology/thumbnail> ?thumbnail . }";
    private final static String WIKI_LINK_SPARQL = "SELECT ?primaryTopic WHERE { <%1$s> <http://xmlns.com/foaf/0.1/isPrimaryTopicOf> ?primaryTopic  . }";
    private final static String DESCRIPTION_SPARQL = "SELECT ?description WHERE { <%1$s> <http://purl.org/dc/terms/description> ?description . FILTER(lang(?description)=\"en\") }";
    private final static String ABSTRACT_SPARQL = "SELECT ?abstract WHERE { <%1$s> <http://dbpedia.org/ontology/abstract> ?abstract .  FILTER(lang(?abstract)=\"en\")  }";


    //TODO siehe chatbot -> SPARQL.getRelevantProperties()

    @RequestMapping(method = RequestMethod.GET, path = "/infobox")
    public String retrieveInfoboxValues(@RequestParam String resource, HttpServletResponse response) {

        JsonObjectBuilder resultObject = Json.createObjectBuilder();
        resultObject = addIfExisting(resultObject, "title", String.format(LABEL_SPARQL, resource));
        resultObject = addIfExisting(resultObject, "description", String.format(DESCRIPTION_SPARQL, resource));
        resultObject = addIfExisting(resultObject, "abstract", String.format(ABSTRACT_SPARQL, resource));
        resultObject = addIfExisting(resultObject, "image", String.format(IMAGE_SPARQL, resource));
        JsonArrayBuilder buttonBuilder = addWikiButtonIfExisting(Json.createArrayBuilder(), resource);
        resultObject.add("buttons", buttonBuilder.add(Json.createObjectBuilder()
                .add("title", "View in DBpedia")
                .add("buttonType", "link")
                .add("uri", extractAnswerString(resource))
                .add("slackStyle", "default")));
        return Json.createObjectBuilder().add("messageData", resultObject).build().toString();
    }

    private JsonArrayBuilder addWikiButtonIfExisting(JsonArrayBuilder arrayBuilder, String resource) {
        final List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(String.format(WIKI_LINK_SPARQL, resource));
        if (sparqlResultSets.size() > 0) {
            final SPARQLResultSet sparqlResultSet = sparqlResultSets.get(0);
            final List<String> resultSet = sparqlResultSet.getResultSet();
            if (resultSet.size() > 0) {
                return arrayBuilder.add(Json.createObjectBuilder()
                        .add("title", "View in Wikipedia")
                        .add("buttonType", "link")
                        .add("uri", extractAnswerString(resultSet.get(0)))
                        .add("slackStyle", "default"));
            }
        }
        return arrayBuilder;
    }

    private JsonObjectBuilder addIfExisting(JsonObjectBuilder objectBuilder, String name, String sparql) {
        final List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(sparql);
        if (sparqlResultSets.size() > 0) {
            final SPARQLResultSet sparqlResultSet = sparqlResultSets.get(0);
            final List<String> resultSet = sparqlResultSet.getResultSet();
            if (resultSet.size() > 0) {
                objectBuilder.add(name, extractAnswerString(resultSet.get(0)));
            }
        }
        return objectBuilder;
    }
}
