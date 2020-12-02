package de.uni.leipzig.tebaqa.template.model;

import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;

public class POTupel {
    private PropertyCandidate predicate;
    private EntityCandidate object;

    public POTupel(PropertyCandidate predicate, EntityCandidate object) {
        this.predicate = predicate;
        this.object = object;
    }

    public PropertyCandidate getPredicate() {
        return predicate;
    }

    public void setPredicate(PropertyCandidate predicate) {
        this.predicate = predicate;
    }

    public EntityCandidate getObject() {
        return object;
    }

    public void setObject(EntityCandidate object) {
        this.object = object;
    }
}
