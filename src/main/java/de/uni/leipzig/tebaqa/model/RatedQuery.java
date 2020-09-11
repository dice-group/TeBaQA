package de.uni.leipzig.tebaqa.model;

import java.util.Arrays;

public class RatedQuery {

    private String query;
    private double rating;

    public RatedQuery(String query, double... ratingValues) {
        this.query = query;
        for(double value : ratingValues) {
            if(value > 0) {
                if(this.rating == 0) {
                    this.rating = 1;
                }
                this.rating = this.rating * value;
            }
        }
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
}
