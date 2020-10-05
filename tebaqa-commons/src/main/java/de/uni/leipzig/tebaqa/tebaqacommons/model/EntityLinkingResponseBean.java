package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Collections;
import java.util.List;

public class EntityLinkingResponseBean {

    private List<ClassCandidate> classCandidates;
    private List<PropertyCandidate> propertyCandidates;
    private List<EntityCandidate> entityCandidates;
    private List<EntityCandidate> literalCandidates;

    public EntityLinkingResponseBean() {
        this.classCandidates = Collections.emptyList();
        this.propertyCandidates = Collections.emptyList();
        this.entityCandidates = Collections.emptyList();
        this.literalCandidates = Collections.emptyList();
    }

    public List<ClassCandidate> getClassCandidates() {
        return classCandidates;
    }

    public void setClassCandidates(List<ClassCandidate> classCandidates) {
        this.classCandidates = classCandidates;
    }

    public List<PropertyCandidate> getPropertyCandidates() {
        return propertyCandidates;
    }

    public void setPropertyCandidates(List<PropertyCandidate> propertyCandidates) {
        this.propertyCandidates = propertyCandidates;
    }

    public List<EntityCandidate> getEntityCandidates() {
        return entityCandidates;
    }

    public void setEntityCandidates(List<EntityCandidate> entityCandidates) {
        this.entityCandidates = entityCandidates;
    }

    public List<EntityCandidate> getLiteralCandidates() {
        return literalCandidates;
    }

    public void setLiteralCandidates(List<EntityCandidate> literalCandidates) {
        this.literalCandidates = literalCandidates;
    }
}
