package de.uni.leipzig.tebaqa.template.model;

import java.util.Objects;

public class TripleTemplate {
    private static final String RESOURCE_PREFIX_IDENTIFIER = "res";
    private final String subject;
    private final String predicate;
    private final String object;
    private String patternString = null;

    public TripleTemplate(String templateString) {
        String[] triple = templateString.split("_");
        subject = triple[0];
        predicate = triple[1];
        object = triple[2];
    }

    public TripleTemplate(String[] triple) {
        subject = triple[0];
        predicate = triple[1];
        object = triple[2];
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

    public boolean isSubjectAResource() {
        return this.subject.startsWith(RESOURCE_PREFIX_IDENTIFIER);
    }

    public boolean isPredicateAResource() {
        return this.predicate.startsWith(RESOURCE_PREFIX_IDENTIFIER);
    }

    public boolean isObjectAResource() {
        return this.object.startsWith(RESOURCE_PREFIX_IDENTIFIER);
    }

    public String getPatternString() {

        if (this.patternString == null) {
            String subject = "v";
            String predicate = "v";
            String object = "v";

            if (this.isSubjectAResource())
                subject = "r";
            if (this.isPredicateAResource())
                predicate = "r";
            if (this.isObjectAResource())
                object = "r";

            this.patternString = String.format("%s_%s_%s", subject, predicate, object);
        }

        return this.patternString;

    }

    public Pattern getPattern() {
        switch (this.getPatternString()) {
            case "v_v_r":
                return Pattern.V_V_R;
            case "v_r_v":
                return Pattern.V_R_V;
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripleTemplate that = (TripleTemplate) o;
        return subject.equals(that.subject) &&
                predicate.equals(that.predicate) &&
                object.equals(that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object);
    }

    @Override
    public String toString() {
        return String.format("TripleTemplate{ %s %s %s }", subject, predicate, object);
    }

    public enum Pattern {

        V_V_R("v_v_r"),
        V_R_V("v_r_v");

        private final String patternString;

        Pattern(String patternString) {
            this.patternString = patternString;
        }

        public boolean sameAs(String anotherPattern) {
            return this.patternString.equalsIgnoreCase(anotherPattern);
        }
    }
}
