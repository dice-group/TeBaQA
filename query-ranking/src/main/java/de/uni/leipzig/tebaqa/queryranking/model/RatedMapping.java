package de.uni.leipzig.tebaqa.queryranking.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RatedMapping {
    private Map<String, String> mappings = new HashMap<>();
    private double rating = 0.0;

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
}
