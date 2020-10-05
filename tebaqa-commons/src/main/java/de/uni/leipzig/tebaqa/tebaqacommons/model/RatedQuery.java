package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Collection;

public class RatedQuery {
    private String query;
    private double score;
    private Collection<ResourceCandidate> usedCandidates;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Collection<ResourceCandidate> getUsedCandidates() {
        return usedCandidates;
    }

    public void setUsedCandidates(Collection<ResourceCandidate> usedCandidates) {
        this.usedCandidates = usedCandidates;
    }
}
