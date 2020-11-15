package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Set;

public interface IResourceCandidate {
    String getCoOccurrence();

    void setCoOccurrence(String coOccurrence);

    void setCoOccurrenceAndScore(String coOccurrence);

    Set<String> getResourceLabels();

    void setResourceLabels(Set<String> resourceLabels);

    String getUri();

    void setUri(String uri);

    Double getLevenshteinSimilarityScore();

    Double getLevenshteinDistanceScore();

    void setLevenshteinDistanceScore(Double levenshteinDistanceScore);

    String getBestLabel();

    void setBestLabel(String bestLabel);

    Double getLinkingScore();

    void setLinkingScore(Double linkingScore);

    Double getRelatednessFactor();

    void setRelatednessFactor(Double relatednessFactor);

    String getBestLabelFor(String coOccurrence);

    double getLevensteinScoreFor(String coOccurrence);
}
