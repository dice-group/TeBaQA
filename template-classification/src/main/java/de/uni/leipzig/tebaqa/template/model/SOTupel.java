package de.uni.leipzig.tebaqa.template.model;

import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;

public class SOTupel {
    private EntityCandidate subject;
    private EntityCandidate object;

    public SOTupel(EntityCandidate subject, EntityCandidate object) {
        this.subject = subject;
        this.object = object;
    }

    public EntityCandidate getObject() {
        return object;
    }

    public void setObject(EntityCandidate object) {
        this.object = object;
    }

    public EntityCandidate getSubject() {
        return subject;
    }

    public void setSubject(EntityCandidate subject) {
        this.subject = subject;
    }
}
