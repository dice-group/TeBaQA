package de.uni.leipzig.tebaqa.modular.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.uni.leipzig.tebaqa.modular.utils.PropertyUtils;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryRankingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryTemplateResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.util.JSONUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class QueryRankingServiceConnector extends AbstractServiceConnector {
    public QueryRankingResponseBean generateQueries(String question, String language, QueryTemplateResponseBean queryTemplates, EntityLinkingResponseBean linkedResources) throws JsonProcessingException {
        String serviceUrl = PropertyUtils.getQueryRankingServiceUrl();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("question", question);
        params.add("lang", language);
        params.addAll("queryTemplates", queryTemplates.getTemplates());
        params.add("linkedResourcesJson", JSONUtils.convertToJSONString(linkedResources));

        ResponseEntity<QueryRankingResponseBean> responseEntity = this.connect(serviceUrl, params, QueryRankingResponseBean.class);
        return responseEntity.getBody();
    }
}