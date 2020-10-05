package de.uni.leipzig.tebaqa.modular.service;

import de.uni.leipzig.tebaqa.modular.utils.PropertyUtils;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class EntityLinkingServiceConnector extends AbstractServiceConnector {

    public EntityLinkingResponseBean extractEntities(String question, String language) {
        String serviceUrl = PropertyUtils.getEntityLinkingServiceUrl();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("question", question);
        params.add("lang", language);

        ResponseEntity<EntityLinkingResponseBean> responseEntity = this.connect(serviceUrl, params, EntityLinkingResponseBean.class);
        return responseEntity.getBody();
    }
}
