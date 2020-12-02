package de.uni.leipzig.tebaqa.model;

import java.util.Set;

public interface ResourceCandidate {


    String getCoOccurence();
    void setCoOccurence(String coOccurence);
    Set<String> getResourceString();
    void setResourceString( Set<String> resourceString);
    String getUri();
    void setUri(String uri);
    Double getLevenstheinScore();
    void setLevenstheinScore(Double levenstheinScore);
    Double getLinkingScore();
    void setLinkingScore(Double linkingScore);
    Double getRelatednessFactor();
    void setRelatednessFactor(Double relatednessFactor);
}