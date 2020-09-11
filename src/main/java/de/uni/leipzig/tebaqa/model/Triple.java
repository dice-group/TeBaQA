package de.uni.leipzig.tebaqa.model;

import org.semarglproject.vocab.RDF;

import java.util.Objects;

public class Triple {

    String subject;
    String predicate;
    String object;
    boolean literalObject;
    double rating;

    public Triple(String subject, String predicate, String object) {
        super();
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.literalObject = false;
        this.rating = 0.0;
    }

    public Triple(String subject, String predicate, String object, boolean literalObject) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.literalObject = literalObject;
        this.rating = 0.0;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public boolean isLiteralObject() {
        return literalObject;
    }

    public void setLiteralObject(boolean literalObject) {
        this.literalObject = literalObject;
    }

    public boolean isPredicateRDFTypeProperty()
    {
        return predicate.equalsIgnoreCase("a") || predicate.equalsIgnoreCase(RDF.TYPE);
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void multiplyRating(double multiplier) {
        if(this.rating == 0.0)
            this.rating = multiplier;
        else {
            if (multiplier != 0)
                this.rating = this.rating * multiplier;
        }
    }

    @Override
    public String toString() {
        return subject + " " + predicate + " " + object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple triple = (Triple) o;
        return subject.equals(triple.subject) &&
                predicate.equals(triple.predicate) &&
                object.equals(triple.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object);
    }
}
