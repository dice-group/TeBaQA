package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Set;

public abstract class ResourceCandidate {

    protected String uri;
    protected String coOccurrence;
    protected Set<String> resourceLabels;
    protected Double levensteinScore;
    protected Double linkingScore;
    protected Double relatednessFactor;


    public String getCoOccurrence() {
        return coOccurrence;
    }

    public void setCoOccurrence(String coOccurrence) {
        this.coOccurrence = coOccurrence;
    }

    public Set<String> getResourceLabels() {
        return resourceLabels;
    }

    public void setResourceLabels(Set<String> resourceLabels) {
        this.resourceLabels = resourceLabels;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Double getLevensteinScore() {
        return levensteinScore;
    }

    public void setLevensteinScore(Double levensteinScore) {
        this.levensteinScore = levensteinScore;
    }

    public Double getLinkingScore() {
        return linkingScore;
    }

    public void setLinkingScore(Double linkingScore) {
        this.linkingScore = linkingScore;
    }

    public Double getRelatednessFactor() {
        return relatednessFactor;
    }

    public void setRelatednessFactor(Double relatednessFactor) {
        this.relatednessFactor = relatednessFactor;
    }

}