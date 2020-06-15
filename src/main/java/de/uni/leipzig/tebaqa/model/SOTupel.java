package de.uni.leipzig.tebaqa.model;

public class SOTupel {
    EntityCandidate subject;
    EntityCandidate object;
    public SOTupel(EntityCandidate subject,EntityCandidate object){
        this.subject=subject;
        this.object=object;
    }

    public EntityCandidate getObject() {
        return object;
    }

    public EntityCandidate getSubject() {
        return subject;
    }

}
