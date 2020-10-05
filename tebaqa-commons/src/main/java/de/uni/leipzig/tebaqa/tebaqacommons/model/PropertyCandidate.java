package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Set;

public class PropertyCandidate extends ResourceCandidate {

    public PropertyCandidate(String uri, Set<String> resourceString) {
        this.uri = uri;
        this.resourceLabels = resourceString;
        this.linkingScore = 0.0;
    }
}
