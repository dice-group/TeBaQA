package de.uni.leipzig.tebaqa.template.model;

import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;

public class SPTupel {
    private EntityCandidate subject;
    private PropertyCandidate predicate;

    public SPTupel(EntityCandidate subject, PropertyCandidate predicate) {
        this.subject = subject;
        this.predicate = predicate;
    }

    public EntityCandidate getSubject() {
        return subject;
    }

    public void setSubject(EntityCandidate subject) {
        this.subject = subject;
    }

    public PropertyCandidate getPredicate() {
        return predicate;
    }

    public void setPredicate(PropertyCandidate predicate) {
        this.predicate = predicate;
    }
}
