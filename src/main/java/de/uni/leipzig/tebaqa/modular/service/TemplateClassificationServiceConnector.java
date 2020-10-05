package de.uni.leipzig.tebaqa.modular.service;


import de.uni.leipzig.tebaqa.modular.utils.PropertyUtils;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryTemplateResponseBean;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class TemplateClassificationServiceConnector extends AbstractServiceConnector {

    public QueryTemplateResponseBean getMatchingQueryTemplates(String question, String language) {

        String serviceUrl = PropertyUtils.getClassificationServiceUrl();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("question", question);
        params.add("lang", language);

        ResponseEntity<QueryTemplateResponseBean> responseEntity = this.connect(serviceUrl, params, QueryTemplateResponseBean.class);
        return responseEntity.getBody();
    }

}
