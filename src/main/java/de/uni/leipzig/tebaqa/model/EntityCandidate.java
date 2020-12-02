package de.uni.leipzig.tebaqa.model;

import java.util.Set;

public class EntityCandidate implements ResourceCandidate{
    private String coOccurence;
    private  Set<String> resourceString;
    private String uri;
    private Set<String> connectedPropertiesSubject;
    private Set<String> connectedPropertiesObject;
    private Set<String> connectedResourcesSubject;
    private Set<String> connectedResourcesObject;
    private Set<String> types;
    private Double levenstheinScore;
    private Double linkingScore;
    private Double relatednessFactor;
    public EntityCandidate(String uri, Set<String> resourceString,Set<String>connectedPropertiesSubject,Set<String>connectedPropertiesObject,
                           Set<String>connectedResourcesSubject,Set<String>connectedResourcesObject,Set<String>types){
        this.uri=uri;
        this.resourceString=resourceString;
        this.connectedPropertiesSubject=connectedPropertiesSubject;
        this.connectedPropertiesObject=connectedPropertiesObject;
        this.connectedResourcesSubject=connectedResourcesSubject;
        this.connectedResourcesObject=connectedResourcesObject;
        this.types=types;
        linkingScore=0.0;
    }


    public String getCoOccurence() {
        return coOccurence;
    }

    public void setCoOccurence(String coOccurence) {
        this.coOccurence = coOccurence;
    }

    public  Set<String> getResourceString() {
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

    public Set<String> getConnectedPropertiesObject() {
        return connectedPropertiesObject;
    }

    public void setConnectedPropertiesObject(Set<String> connectedProperties) {
        this.connectedPropertiesObject = connectedProperties;
    }
    public Set<String> getConnectedPropertiesSubject() {
        return connectedPropertiesSubject;
    }

    public void setConnectedPropertiesSubject(Set<String> connectedProperties) {
        this.connectedPropertiesSubject = connectedProperties;
    }

    public Set<String> getConnectedResourcesSubject() {
        return connectedResourcesSubject;
    }

    public void setConnectedResources(Set<String> connectedResources) {
        this.connectedResourcesSubject = connectedResources;
    }
    public Set<String> getConnectedResourcesObject() {
        return connectedResourcesObject;
    }

    public void setConnectedResourcesObject(Set<String> connectedResources) {
        this.connectedResourcesObject = connectedResources;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
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
