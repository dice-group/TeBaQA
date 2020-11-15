package de.uni.leipzig.tebaqa.tebaqacommons.model;

import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;

import java.util.Set;

public abstract class ResourceCandidate implements IResourceCandidate {

    protected String uri;
    protected String coOccurrence;
    protected Set<String> resourceLabels;
    protected Double levenshteinDistanceScore;
    protected Double linkingScore;
    protected Double relatednessFactor;
    protected String bestLabel;


    @Override
    public String getCoOccurrence() {
        return coOccurrence;
    }

    @Override
    public void setCoOccurrence(String coOccurrence) {
        this.coOccurrence = coOccurrence;
    }

    @Override
    public void setCoOccurrenceAndScore(String coOccurrence) {
        this.coOccurrence = coOccurrence;
        double bestScore = 1;
        String bestScoreLabel = null;
        for (String label : this.getResourceLabels()) {
            double levensteinScore = TextUtilities.getLevenshteinRatio(coOccurrence, label);
            if (levensteinScore < bestScore) {
                bestScore = levensteinScore;
                bestScoreLabel = label;
            }
        }
        this.levenshteinDistanceScore = bestScore;
        this.bestLabel = bestScoreLabel;
    }

    @Override
    public Set<String> getResourceLabels() {
        return resourceLabels;
    }

    @Override
    public void setResourceLabels(Set<String> resourceLabels) {
        this.resourceLabels = resourceLabels;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public Double getLevenshteinSimilarityScore() {
        if (this.levenshteinDistanceScore != null)
            return 1 - this.levenshteinDistanceScore;
        else
            return null;
    }

    @Override
    public Double getLevenshteinDistanceScore() {
        return levenshteinDistanceScore;
    }

    @Override
    public void setLevenshteinDistanceScore(Double levenshteinDistanceScore) {
        this.levenshteinDistanceScore = levenshteinDistanceScore;
    }

    @Override
    public String getBestLabel() {
        return bestLabel;
    }

    @Override
    public void setBestLabel(String bestLabel) {
        this.bestLabel = bestLabel;
    }

    @Override
    public Double getLinkingScore() {
        return linkingScore;
    }

    @Override
    public void setLinkingScore(Double linkingScore) {
        this.linkingScore = linkingScore;
    }

    @Override
    public Double getRelatednessFactor() {
        return relatednessFactor;
    }

    @Override
    public void setRelatednessFactor(Double relatednessFactor) {
        this.relatednessFactor = relatednessFactor;
    }

    @Override
    public double getLevensteinScoreFor(String coOccurrence) {
        double bestScore = 1;
        String bestScoreLabel = null;
        for (String label : this.getResourceLabels()) {
            double levensteinScore = TextUtilities.getLevenshteinRatio(coOccurrence, label);
            if (levensteinScore < bestScore) {
                bestScore = levensteinScore;
                bestScoreLabel = label;
            }
        }
        return bestScore;
    }

    @Override
    public String getBestLabelFor(String coOccurrence) {
        double bestScore = 1;
        String bestScoreLabel = null;
        for (String label : this.getResourceLabels()) {
            double levensteinScore = TextUtilities.getLevenshteinRatio(coOccurrence, label);
            if (levensteinScore < bestScore) {
                bestScore = levensteinScore;
                bestScoreLabel = label;
            }
        }
        return bestScoreLabel;
    }
}