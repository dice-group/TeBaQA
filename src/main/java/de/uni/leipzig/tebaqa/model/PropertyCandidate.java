package de.uni.leipzig.tebaqa.model;

import java.util.Set;

public class PropertyCandidate implements ResourceCandidate{
    private String coOccurence;
    private  Set<String> resourceString;
    private String uri;
    private Double levenstheinScore;
    private Double linkingScore;
    private Double relatednessFactor;
    public PropertyCandidate(String uri, Set<String> resourceString){
        this.coOccurence=coOccurence;
        this.uri=uri;
        this.resourceString=resourceString;
        linkingScore=0.0;
    }



    public String getCoOccurence() {
        return coOccurence;
    }

    public void setCoOccurence(String coOccurence) {
        this.coOccurence = coOccurence;
    }

    public Set<String> getResourceString() {
        return resourceString;
    }

    public void setResourceString( Set<String> resourceString) {
        this.resourceString = resourceString;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Double getLevenstheinScore() {
        return levenstheinScore;
    }

    public void setLevenstheinScore(Double levenstheinScore) {
        this.levenstheinScore = levenstheinScore;
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
