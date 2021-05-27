package de.uni.leipzig.tebaqa.tebaqacontroller.service;


import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryTemplateResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacontroller.utils.ControllerPropertyUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

public class TemplateClassificationServiceConnector extends AbstractServiceConnector {

    public QueryTemplateResponseBean getMatchingQueryTemplates(String question, Lang language) {
//        if(ControllerPropertyUtils.ablationClassification()) {
//            return ablation(question, language);
//        } else {
            String serviceUrl = ControllerPropertyUtils.getClassificationServiceUrl();

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("question", question);
            params.add("lang", language.getLanguageCode());

            ResponseEntity<QueryTemplateResponseBean> responseEntity = this.connect(serviceUrl, params, QueryTemplateResponseBean.class);
            return responseEntity.getBody();
//        }
    }

//    public QueryTemplateResponseBean ablation(String question, Lang language) {
//        QueryTemplateResponseBean response = new QueryTemplateResponseBean();
//        response.setQuestion(question);
//        response.setLang(language.getLanguageCode());
//        response.setTemplates(AblationProvider.getTemplates(question));
//        return response;
//    }

    public QueryTemplateResponseBean getAllQueryTemplates(String question, Lang lang) {
//        if(ControllerPropertyUtils.ablationClassification()) {
//            return ablation(question, lang);
//        } else {
            String serviceUrl = ControllerPropertyUtils.getAllTemplatesServiceUrl();

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("question", question);
            params.add("lang", lang.getLanguageCode());

            ResponseEntity<List> responseEntity = this.connect(serviceUrl, params, List.class);
            List<String> allTemplates = responseEntity.getBody();
            QueryTemplateResponseBean response = new QueryTemplateResponseBean();
            response.setQuestion(question);
            response.setLang(lang.getLanguageCode());
            response.setTemplates(allTemplates);
            return response;
//        }
    }
}
