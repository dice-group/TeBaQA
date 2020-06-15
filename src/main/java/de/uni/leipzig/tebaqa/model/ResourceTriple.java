package de.uni.leipzig.tebaqa.model;

public class ResourceTriple {
    private ResourceCandidate subject=null;
    private ResourceCandidate predicate=null;
    private ResourceCandidate object=null;


    public ResourceCandidate getSubject() {
        return subject;
    }

    public void setSubject(ResourceCandidate subject) {
        this.subject = subject;
    }

    public ResourceCandidate getPredicate() {
        return predicate;
    }

    public void setPredicate(ResourceCandidate predicate) {
        this.predicate = predicate;
    }

    public ResourceCandidate getObject() {
        return object;
    }

    public void setObject(ResourceCandidate object) {
        this.object = object;
    }
}
