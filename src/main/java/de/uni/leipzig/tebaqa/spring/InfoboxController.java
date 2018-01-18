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

import static de.uni.leipzig.tebaqa.helper.WikiTextUtilities.stripWikipediaContent;
import static de.uni.leipzig.tebaqa.spring.ExtendedQALDAnswer.extractAnswerString;

@RestController
public class InfoboxController {

    private static Logger log = Logger.getLogger(InfoboxController.class.getName());

    //TODO Extract properties from DBpedia entity like in dbpedia-chatbot -> SPARQL.getRelevantProperties()

    @RequestMapping(method = RequestMethod.GET, path = "/infobox")
    public String retrieveInfoboxValues(@RequestParam String resource, HttpServletResponse response) {
        log.debug(String.format("/infobox received GET request with: resource='%s'", resource));
        JsonObjectBuilder resultObject = Json.createObjectBuilder();
        resultObject = addIfExisting(resultObject, "title", String.format(SPARQLUtilities.LABEL_SPARQL, resource), false);
        resultObject = addIfExisting(resultObject, "description", String.format(SPARQLUtilities.DESCRIPTION_SPARQL, resource), true);
        resultObject = addIfExisting(resultObject, "abstract", String.format(SPARQLUtilities.ABSTRACT_SPARQL, resource), true);
        resultObject = addIfExisting(resultObject, "image", String.format(SPARQLUtilities.IMAGE_SPARQL, resource), false);
        JsonArrayBuilder buttonBuilder = addWikiButtonIfExisting(Json.createArrayBuilder(), resource);
        resultObject.add("buttons", buttonBuilder.add(Json.createObjectBuilder()
                .add("title", "View in DBpedia")
                .add("buttonType", "link")
                .add("uri", extractAnswerString(resource))
                .add("slackStyle", "default")));
        return Json.createObjectBuilder().add("messageData", resultObject).build().toString();
    }

    private JsonArrayBuilder addWikiButtonIfExisting(JsonArrayBuilder arrayBuilder, String resource) {
        final List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(String.format(SPARQLUtilities.WIKI_LINK_SPARQL, resource));
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

    private JsonObjectBuilder addIfExisting(JsonObjectBuilder objectBuilder, String name, String sparql, boolean stripContent) {
        final List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(sparql);
        if (sparqlResultSets.size() > 0) {
            final SPARQLResultSet sparqlResultSet = sparqlResultSets.get(0);
            final List<String> resultSet = sparqlResultSet.getResultSet();
            if (resultSet.size() > 0) {
                if (stripContent) {
                    objectBuilder.add(name, stripWikipediaContent(extractAnswerString(resultSet.get(0))));
                } else {
                    objectBuilder.add(name, extractAnswerString(resultSet.get(0)));
                }
            }
        }
        return objectBuilder;
    }
}
