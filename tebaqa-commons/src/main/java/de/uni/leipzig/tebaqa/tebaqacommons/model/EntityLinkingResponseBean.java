package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Collections;
import java.util.Set;

public class EntityLinkingResponseBean {

    private Set<ClassCandidate> classCandidates;
    private Set<PropertyCandidate> propertyCandidates;
    private Set<EntityCandidate> entityCandidates;
    private Set<EntityCandidate> literalCandidates;

    public EntityLinkingResponseBean() {
        this.classCandidates = Collections.emptySet();
        this.propertyCandidates = Collections.emptySet();
        this.entityCandidates = Collections.emptySet();
        this.literalCandidates = Collections.emptySet();
    }

    public EntityLinkingResponseBean(Set<ClassCandidate> classCandidates, Set<PropertyCandidate> propertyCandidates, Set<EntityCandidate> entityCandidates) {
        this.classCandidates = classCandidates;
        this.propertyCandidates = propertyCandidates;
        this.entityCandidates = entityCandidates;
        this.literalCandidates = Collections.emptySet();
    }

    public Set<ClassCandidate> getClassCandidates() {
        return classCandidates;
    }

    public void setClassCandidates(Set<ClassCandidate> classCandidates) {
        this.classCandidates = classCandidates;
    }

    public Set<PropertyCandidate> getPropertyCandidates() {
        return propertyCandidates;
    }

    public void setPropertyCandidates(Set<PropertyCandidate> propertyCandidates) {
        this.propertyCandidates = propertyCandidates;
    }

    public Set<EntityCandidate> getEntityCandidates() {
        return entityCandidates;
    }

    public void setEntityCandidates(Set<EntityCandidate> entityCandidates) {
        this.entityCandidates = entityCandidates;
    }

    public Set<EntityCandidate> getLiteralCandidates() {
        return literalCandidates;
    }

    public void setLiteralCandidates(Set<EntityCandidate> literalCandidates) {
        this.literalCandidates = literalCandidates;
    }
}
