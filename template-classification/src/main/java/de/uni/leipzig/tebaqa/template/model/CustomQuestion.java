package de.uni.leipzig.tebaqa.template.model;

import java.util.List;
import java.util.Map;

public class CustomQuestion {
    private final List<String> modifiers;
    private final String query;
    private final String questionText;
    private final String graph;
    private Map<String, String> dependencySequencePosMap;

    public CustomQuestion(String query, String questionText, List<String> simpleModifiers, String graph) {
        this.query = query;
        this.questionText = questionText;
        this.modifiers = simpleModifiers;
        this.graph = graph;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public String getQuery() {
        return query;
    }

    public String getGraph() {
        return graph;
    }

    public Map<String, String> getDependencySequencePosMap() {
        return dependencySequencePosMap;
    }

    public void setDependencySequencePosMap(Map<String, String> dependencySequencePosMap) {
        this.dependencySequencePosMap = dependencySequencePosMap;
    }

    @Override
    public String toString() {
        return "CustomQuestion{" +
                "modifiers=" + modifiers +
                ", query='" + query + '\'' +
                ", questionText='" + questionText + '\'' +
                ", graph='" + graph + '\'' +
                ", dependencySequencePosMap=" + dependencySequencePosMap +
                '}';
    }
}
