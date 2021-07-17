package de.uni.leipzig.tebaqa.template.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryTemplateResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryType;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.template.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.template.service.WekaClassifier;
import de.uni.leipzig.tebaqa.template.util.Constants;
import de.uni.leipzig.tebaqa.template.util.PropertyUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class TemplateClassificationController {

    private static final Logger LOGGER = LogManager.getLogger(TemplateClassificationController.class.getName());
    private static final WekaClassifier classifier;
    private static final SemanticAnalysisHelper semanticAnalysisHelper;

    static {
        SemanticAnalysisHelper helper;
        WekaClassifier weka;
        try {
            helper = Lang.EN.getSemanticAnalysisHelper();
        } catch (IOException e) {
            e.printStackTrace();
            helper = null;
        }
        semanticAnalysisHelper = helper;

        try {
            weka = WekaClassifier.getDefaultClassifier();
        } catch (IOException e) {
            e.printStackTrace();
            weka = null;
        }
        classifier = weka;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, path = "/templates")
    public Set<String> getAllTemplates(@RequestParam String question,
                                       @RequestParam(required = false, defaultValue = "en") String lang,
                                       HttpServletResponse response) {
        LOGGER.info("GET for /templates");

        QueryType queryType = semanticAnalysisHelper.mapQuestionToQueryType(question);
        LOGGER.info(String.format("Query Type: %s", queryType.name()));
        Set<String> allTemplates = classifier.getAllQueryTemplates().stream().flatMap(queryTemplateMapping -> queryTemplateMapping.getTemplatesFor(queryType).stream()).collect(Collectors.toSet());

        return allTemplates;
    }


    @RequestMapping(method = RequestMethod.POST, path = "/classify-template")
    public QueryTemplateResponseBean classifyTemplate(@RequestParam String question,
                                                      @RequestParam(required = false, defaultValue = "en") String lang,
                                                      HttpServletResponse response) throws JsonProcessingException {
        LOGGER.info(String.format("/classify-template received POST request with: question='%s'", question));

        if (question.isEmpty()) {
            LOGGER.error("Received request with empty query parameter!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a valid question");
        }

        QueryTemplateResponseBean templateResponseBean = new QueryTemplateResponseBean();
        templateResponseBean.setQuestion(question);
        templateResponseBean.setLang(lang);

        String graph = classifier.classifyInstance(question);
        LOGGER.info(String.format("%s -> %s", question, graph));

        QueryType queryType = semanticAnalysisHelper.mapQuestionToQueryType(question);
        QueryTemplateMapping templateMapping = classifier.getQueryTemplatesFor(graph);
        LOGGER.info(String.format("Query Type: %s", queryType.name()));

        Set<String> templates;
        boolean forceResult = "true".equalsIgnoreCase(PropertyUtils.getProperty(Constants.FORCE_CLASSIFICATION_RESPONSE));
        if (templateMapping == null) {
            // In case QueryTemplateMapping cannot be found for the classified graph then,
            // from all QueryTemplateMapping, get the templates of the given queryType.
            LOGGER.info("Template mapping not found");
            if (forceResult) {
                templates = classifier.getAllQueryTemplates().stream().flatMap(queryTemplateMapping -> queryTemplateMapping.getTemplatesFor(queryType).stream()).collect(Collectors.toSet());
            } else {
                templates = Collections.emptySet();
            }
        }
        else {
            // If the QueryTemplateMapping is found for the classified graph,
            // then get the templates from that single QueryTemplateMapping
            if(forceResult)
                templates = templateMapping.getTemplatesFor(queryType); // Gets all available template in case query type is unknown
            else {
                if(queryType == QueryType.QUERY_TYPE_UNKNOWN)
                    templates = Collections.emptySet();
                else
                    templates = templateMapping.getTemplatesFor(queryType);
            }
        }
        LOGGER.info("Force response: " + forceResult);
        LOGGER.info("Total templates: " + templates.size());
        templateResponseBean.setTemplates(new ArrayList<>(templates));

//        return ResponseEntity.status(HttpStatus.OK).body(JSONUtils.convertToJSONString(templateResponseBean)).toString();
        return templateResponseBean;
    }


}
