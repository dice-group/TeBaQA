package de.uni.leipzig.tebaqa.model;

public class SPTupel {
    EntityCandidate subject;
    PropertyCandidate predicate;
    public SPTupel(EntityCandidate subject,PropertyCandidate predicate){
        this.subject=subject;
        this.predicate=predicate;
    }

    public EntityCandidate getSubject() {
        return subject;
    }

    public PropertyCandidate getPredicate() {
        return predicate;
    }

}
