package de.uni.leipzig.tebaqa.queryranking.spring;

import de.uni.leipzig.tebaqa.queryranking.core.QueryGenerator;
import de.uni.leipzig.tebaqa.queryranking.model.EntityLinkingResult;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryRankingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.util.JSONUtils;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

@RestController
public class QueryRankingController {

    private static final Logger LOGGER = Logger.getLogger(QueryRankingController.class.getName());

    @RequestMapping(method = RequestMethod.POST, path = "/generate-queries")
    public QueryRankingResponseBean entityLinking(@RequestParam String question,
                                                  @RequestParam String lang,
                                                  @RequestParam Set<String> queryTemplates,
                                                  @RequestParam String linkedResourcesJson,
                                                  HttpServletResponse servletResponse) throws IOException {
        LOGGER.info(String.format("/generate-queries received POST request with: question='%s' & lang=%s & %s query templates", question, lang, queryTemplates.size()));

        Lang language = Lang.getForCode(lang);
        if (question.isEmpty() || language == null || queryTemplates == null || queryTemplates.isEmpty()) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters provided!");
        }

        EntityLinkingResponseBean linkedResourcesBean = JSONUtils.JSONStringToObject(linkedResourcesJson, EntityLinkingResponseBean.class);
        EntityLinkingResult linkedEntities = new EntityLinkingResult(linkedResourcesBean);
        QueryGenerator queryGenerator = new QueryGenerator(linkedEntities, queryTemplates);
        return queryGenerator.generateQueries();
    }


}
