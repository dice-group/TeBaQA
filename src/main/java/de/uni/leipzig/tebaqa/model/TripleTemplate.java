package de.uni.leipzig.tebaqa.model;

public class TripleTemplate {
    private String subject;
    private String predicate;
    private String object;
    public TripleTemplate(String templateString){
        String[]triple=templateString.split("_");
        subject=triple[0];
        predicate=triple[1];
        object=triple[2];
    }
    public TripleTemplate(String[]triple){
        subject=triple[0];
        predicate=triple[1];
        object=triple[2];
    }

    public String getSubject() {
        return subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }
}
