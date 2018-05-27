package de.uni.leipzig.tebaqa.model;

import org.jetbrains.annotations.NotNull;

public class RatedEntity implements Comparable {
    private String uri;
    private String origin;
    private Double rating;

    public RatedEntity(String uri, String origin, Double rating) {
        this.uri = uri;
        this.origin = origin;
        this.rating = rating;
    }

    public String getUri() {
        return uri;
    }

    public String getOrigin() {
        return origin;
    }

    public Double getRating() {
        return rating;
    }

    @Override
    public int compareTo(@NotNull Object anotherRatedEntity) {
        if (!(anotherRatedEntity instanceof RatedEntity)) {
            throw new ClassCastException("A RatedEntity object is expected.");
        }
        return Double.compare(this.rating, ((RatedEntity) anotherRatedEntity).getRating());
    }
}
