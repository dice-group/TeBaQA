package de.uni.leipzig.tebaqa.tebaqacontroller.service;


import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryTemplateResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacontroller.utils.ControllerPropertyUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class TemplateClassificationServiceConnector extends AbstractServiceConnector {

    public QueryTemplateResponseBean getMatchingQueryTemplates(String question, Lang language) {

        String serviceUrl = ControllerPropertyUtils.getClassificationServiceUrl();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("question", question);
        params.add("lang", language.getLanguageCode());

        ResponseEntity<QueryTemplateResponseBean> responseEntity = this.connect(serviceUrl, params, QueryTemplateResponseBean.class);
        return responseEntity.getBody();
    }

}
