package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Collection;

public class RatedQuery {

    private String queryTemplate;
    private String query;
    private double rating;

    public RatedQuery(String query, double... ratingValues) {
        this.query = query;
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

    @Override
    public String toString() {
//        return "RatedQuery{" +
//                "query='" + query + '\'' +
//                ", rating=" + rating +
//                '}';
        return String.format("%s --> %s", rating, query);
    }
}
