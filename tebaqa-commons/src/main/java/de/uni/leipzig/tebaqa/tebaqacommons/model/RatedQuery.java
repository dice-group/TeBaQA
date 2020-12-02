package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RatedQuery {

    private String queryTemplate;
    private String query;
    private double rating;
    private Set<EntityCandidate> usedEntities;
    private Set<PropertyCandidate> usedProperties;
    private Set<ClassCandidate> usedClasses;


    private RatedQuery() {}

    public RatedQuery(String query, Collection<EntityCandidate> usedEntities,
                      Collection<PropertyCandidate> usedProperties,
                      Collection<ClassCandidate> usedClasses, double... ratingValues) {
        this.query = query;
        this.usedEntities = new HashSet<>(usedEntities);
        this.usedProperties = new HashSet<>(usedProperties);
        this.usedClasses = new HashSet<>(usedClasses);
        for (double value : ratingValues) {
            if (value > 0) {
                if (this.rating == 0) {
                    this.rating = 1;
                }
                this.rating = this.rating * value;
            }
        }
    }

    public String getQueryTemplate() {
        return queryTemplate;
    }

    public void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public Set<EntityCandidate> getUsedEntities() {
        return usedEntities;
    }

    public void setUsedEntities(Set<EntityCandidate> usedEntities) {
        this.usedEntities = usedEntities;
    }

    public Set<PropertyCandidate> getUsedProperties() {
        return usedProperties;
    }

    public void setUsedProperties(Set<PropertyCandidate> usedProperties) {
        this.usedProperties = usedProperties;
    }

    public Set<ClassCandidate> getUsedClasses() {
        return usedClasses;
    }

    public void setUsedClasses(Set<ClassCandidate> usedClasses) {
        this.usedClasses = usedClasses;
    }

    @Override
    public String toString() {
//        return "RatedQuery{" +
//                "query='" + query + '\'' +
//                ", rating=" + rating +
//                '}';
        return String.format("%s --> %s", rating, query);
    }
}
