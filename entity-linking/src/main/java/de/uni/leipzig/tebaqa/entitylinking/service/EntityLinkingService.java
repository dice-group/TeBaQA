package de.uni.leipzig.tebaqa.entitylinking.service;

import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;

import java.io.IOException;

public class EntityLinkingService {

    public EntityLinkingResponseBean findEntitiesFrom(String question, Lang lang) throws IOException {
        ResourceLinker resourceLinker = new ResourceLinker(question, lang);
        resourceLinker.linkEntities();
        return new EntityLinkingResponseBean(resourceLinker.getCoOccurrences(), resourceLinker.getClassCandidates(),
                resourceLinker.getPropertyCandidates(), resourceLinker.getEntityCandidates(), resourceLinker.getPropertyUris());
    }

}
