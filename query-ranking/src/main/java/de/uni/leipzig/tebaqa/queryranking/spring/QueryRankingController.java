package de.uni.leipzig.tebaqa.queryranking.spring;

import de.uni.leipzig.tebaqa.queryranking.core.QueryGenerator;
import de.uni.leipzig.tebaqa.queryranking.model.EntityLinkingResult;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryRankingRequestBody;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryRankingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;

@RestController
public class QueryRankingController {

    private static final Logger LOGGER = LogManager.getLogger(QueryRankingController.class.getName());

    @PostMapping(
            value = "/generate-queries",
            produces = {"application/hal+json", "application/json"}
    )
    public QueryRankingResponseBean generateQueries(@RequestBody QueryRankingRequestBody body) throws IOException {
        String question = body.getQuestion();
        String lang = body.getLang();
        Set<String> queryTemplates = body.getQueryTemplates();
        EntityLinkingResponseBean linkedResourcesBean = body.getLinkedResourcesJson();

        LOGGER.info(String.format("/generate-queries received POST request with: question='%s' & lang=%s & %s query templates", question, lang, queryTemplates.size()));

        Lang language = Lang.getForCode(lang);
        if (question.isEmpty() || language == null || queryTemplates == null || queryTemplates.isEmpty()) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

//        EntityLinkingResponseBean linkedResourcesBean = JSONUtils.JSONStringToObject(linkedResourcesJson, EntityLinkingResponseBean.class);
        EntityLinkingResult linkedEntities = new EntityLinkingResult(linkedResourcesBean);
        QueryGenerator queryGenerator = new QueryGenerator(linkedEntities, queryTemplates);
        QueryRankingResponseBean generatedQueries = queryGenerator.generateQueries();
        printInfos(generatedQueries);
        return generatedQueries;
    }

    private static void printInfos(QueryRankingResponseBean generatedQueries) {
        LOGGER.info("Query generation finished");
        generatedQueries.getGeneratedQueries().forEach(LOGGER::info);
    }
}
