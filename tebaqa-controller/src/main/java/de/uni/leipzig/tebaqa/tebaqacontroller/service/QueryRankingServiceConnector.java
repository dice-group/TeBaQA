package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryRankingRequestBody;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryRankingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryTemplateResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacontroller.utils.ControllerPropertyUtils;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;

public class QueryRankingServiceConnector extends AbstractServiceConnector {
    public QueryRankingResponseBean generateQueries(String question, Lang language, QueryTemplateResponseBean queryTemplates, EntityLinkingResponseBean linkedResources) throws JsonProcessingException, JSONException {
        String serviceUrl = ControllerPropertyUtils.getQueryRankingServiceUrl();

        QueryRankingRequestBody requestBody = new QueryRankingRequestBody(question, language.getLanguageCode(), new HashSet<>(queryTemplates.getTemplates()), linkedResources);

        ResponseEntity<QueryRankingResponseBean> responseEntity = this.connectPostJson(serviceUrl, requestBody, QueryRankingResponseBean.class);
        return responseEntity.getBody();
    }

}
