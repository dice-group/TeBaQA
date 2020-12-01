package de.uni.leipzig.tebaqa.queryranking.model;


import de.uni.leipzig.tebaqa.tebaqacommons.model.ClassCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ResourceCandidate;
import org.apache.jena.vocabulary.RDF;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Triple {

    private String subject;
    private String predicate;
    private String object;
    private ResourceCandidate subjectCandidate;
    private ResourceCandidate predicateCandidate;
    private ResourceCandidate objectCandidate;
    private boolean literalObject;
    private double rating;
    private Set<EntityCandidate> usedEntities;
    private Set<PropertyCandidate> usedProperties;
    private Set<ClassCandidate> usedClasses;

    public Triple(String subject, String predicate, String object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.literalObject = false;
        this.rating = 0.0;
        this.usedEntities = new HashSet<>();
        this.usedProperties = new HashSet<>();
        this.usedClasses = new HashSet<>();
    }

    public Triple(String subject, String predicate, String object, boolean literalObject) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.literalObject = literalObject;
        this.rating = 0.0;
        this.usedEntities = new HashSet<>();
        this.usedProperties = new HashSet<>();
        this.usedClasses = new HashSet<>();
    }

    public Triple(ResourceCandidate subject, ResourceCandidate predicate, ResourceCandidate object) {
        this(subject, predicate, object, false);
    }

    public Triple(ResourceCandidate subject, ResourceCandidate predicate, ResourceCandidate object, boolean literalObject) {
        this(getUri(subject), getUri(predicate), getUri(object), literalObject);
        this.subjectCandidate = subject;
        this.predicateCandidate = predicate;
        this.objectCandidate = object;
        this.rating = calculateRating(subject, predicate, object);
    }

    private static String getUri(ResourceCandidate resource) {
        return resource == null ? TripleTemplate.VARIABLE_PLACEHOLDER : resource.getUri();
    }

    /* Calculates the rating based on average of levenstein similarity scores of s, p, o */
    private static double calculateRating(ResourceCandidate subjectCandidate, ResourceCandidate predicateCandidate, ResourceCandidate objectCandidate) {
        int totalResources = 0;
        double totalLevensteinSim = 0;
        if (subjectCandidate != null && subjectCandidate.getDistanceScore() != null) {
            totalResources++;
            totalLevensteinSim += (1 - subjectCandidate.getDistanceScore());
        }
        if (predicateCandidate != null && predicateCandidate.getDistanceScore() != null) {
            totalResources++;
            totalLevensteinSim += (1 - predicateCandidate.getDistanceScore());
        }
        if (objectCandidate != null && objectCandidate.getDistanceScore() != null) {
            totalResources++;
            totalLevensteinSim += (1 - objectCandidate.getDistanceScore());
        }

        return totalResources > 0 ? (totalLevensteinSim / totalResources) : 0;
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

    public ResourceCandidate getSubjectCandidate() {
        return subjectCandidate;
    }

    public void setSubjectCandidate(ResourceCandidate subjectCandidate) {
        this.subjectCandidate = subjectCandidate;
    }

    public ResourceCandidate getPredicateCandidate() {
        return predicateCandidate;
    }

    public void setPredicateCandidate(ResourceCandidate predicateCandidate) {
        this.predicateCandidate = predicateCandidate;
    }

    public ResourceCandidate getObjectCandidate() {
        return objectCandidate;
    }

    public void setObjectCandidate(ResourceCandidate objectCandidate) {
        this.objectCandidate = objectCandidate;
    }

    public boolean isLiteralObject() {
        return literalObject;
    }

    public void setLiteralObject(boolean literalObject) {
        this.literalObject = literalObject;
    }

    public boolean isPredicateRDFTypeProperty() {
        return predicate.equalsIgnoreCase("a") || predicate.equalsIgnoreCase(RDF.type.getURI());
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void multiplyRating(double multiplier) {
        if (this.rating == 0.0)
            this.rating = multiplier;
        else {
            if (multiplier > 0)
                this.rating = this.rating * multiplier;
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

    public void addUsedEntity(EntityCandidate entity) {
        this.usedEntities.add(entity);
    }

    public void addUsedClass(ClassCandidate clazz) {
        this.usedClasses.add(clazz);
    }

    public void addUsedProperty(PropertyCandidate property) {
        this.usedProperties.add(property);
    }
}
