package de.uni.leipzig.tebaqa.queryranking.model;

import de.uni.leipzig.tebaqa.tebaqacommons.model.ClassCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;

import java.util.*;

public class RatedMapping {
    private Map<String, String> mappings;
    private double rating;
    private Set<EntityCandidate> usedEntities;
    private Set<PropertyCandidate> usedProperties;
    private Set<ClassCandidate> usedClasses;

    public RatedMapping() {
        mappings = new HashMap<>();
        rating = 0.0;
        usedEntities = new HashSet<>();
        usedProperties = new HashSet<>();
        usedClasses = new HashSet<>();
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String put(String key, String value) {
        return this.mappings.put(key, value);
    }

    public Set<String> keySet() {
        return this.mappings.keySet();
    }

    public String get(String key) {
        return this.mappings.get(key);
    }

    public Collection<String> values() {
        return this.mappings.values();
    }

    public void putAll(Map<String, String> newMappings) {
        this.mappings.putAll(newMappings);
    }

    public int size() {
        return this.mappings.size();
    }

    public void multiplyRating(double rating) {
        if (this.rating == 0)
            this.rating = rating;
        else {
            if (rating > 0)
                this.rating = this.rating * rating;
        }
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

    public void addUsedEntities(Collection<EntityCandidate> entities) {
        this.usedEntities.addAll(entities);
    }

    public void addUsedClasses(Collection<ClassCandidate> classes) {
        this.usedClasses.addAll(classes);
    }

    public void addUsedProperties(Collection<PropertyCandidate> properties) {
        this.usedProperties.addAll(properties);
    }


}
