package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Set;

public class EntityCandidate extends ResourceCandidate {
    private Set<String> connectedPropertiesSubject;
    private Set<String> connectedPropertiesObject;
    private Set<String> connectedResourcesSubject;
    private Set<String> connectedResourcesObject;
    private Set<String> types;

    public EntityCandidate(String uri, Set<String> resourceString, Set<String> connectedPropertiesSubject, Set<String> connectedPropertiesObject,
                           Set<String> connectedResourcesSubject, Set<String> connectedResourcesObject, Set<String> types) {
        this.uri = uri;
        this.resourceLabels = resourceString;
        this.connectedPropertiesSubject = connectedPropertiesSubject;
        this.connectedPropertiesObject = connectedPropertiesObject;
        this.connectedResourcesSubject = connectedResourcesSubject;
        this.connectedResourcesObject = connectedResourcesObject;
        this.types = types;
        this.linkingScore = 0.0;
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

}
