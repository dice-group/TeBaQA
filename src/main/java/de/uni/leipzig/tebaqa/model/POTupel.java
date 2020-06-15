package de.uni.leipzig.tebaqa.model;

public class POTupel {
    PropertyCandidate predicate;
    EntityCandidate object;
    public POTupel(PropertyCandidate predicate,EntityCandidate object){
        this.predicate=predicate;
        this.object=object;
    }

    public PropertyCandidate getPredicate() {
        return predicate;
    }

    public EntityCandidate getObject() {
        return object;
    }

}
