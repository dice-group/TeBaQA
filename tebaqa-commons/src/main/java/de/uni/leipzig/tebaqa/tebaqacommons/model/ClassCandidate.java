package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Set;

public class ClassCandidate extends ResourceCandidate {

    public ClassCandidate(String uri, Set<String> resourceString) {
        this.uri = uri;
        this.resourceLabels = resourceString;
    }

}
