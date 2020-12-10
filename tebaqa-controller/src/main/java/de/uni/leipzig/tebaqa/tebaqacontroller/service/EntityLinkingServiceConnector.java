package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacontroller.utils.ControllerPropertyUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class EntityLinkingServiceConnector extends AbstractServiceConnector {

    public EntityLinkingResponseBean extractEntities(String question, Lang language) {
//        if(ControllerPropertyUtils.ablationEL()) {
//            return ablation(question);
//        }
//        else {
            String serviceUrl = ControllerPropertyUtils.getEntityLinkingServiceUrl();

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("question", question);
            params.add("lang", language.getLanguageCode());

            ResponseEntity<EntityLinkingResponseBean> responseEntity = this.connect(serviceUrl, params, EntityLinkingResponseBean.class);
            return responseEntity.getBody();
//        }
    }

//    private EntityLinkingResponseBean ablation(String question) {
//        CustomQuestion enrichedQuestion = AblationProvider.getEnrichedQuestion(question);
//        EntityLinkingResponseBean entityLinkingResponseBean = new EntityLinkingResponseBean();
//
//        if(enrichedQuestion == null)
//            return entityLinkingResponseBean;
//
//        return enrichedQuestion.getLinkedEntities();
//    }
}
