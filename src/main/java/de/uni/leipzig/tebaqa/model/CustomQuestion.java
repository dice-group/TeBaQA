package de.uni.leipzig.tebaqa.model;

import java.util.List;

public class CustomQuestion {
    private List<String> modifiers;
    private String query;

    public String getQuestionText() {
        return questionText;
    }

    private String questionText;
    private String graph;

    public List<String> getModifiers() {
        return modifiers;
    }

    public String getQuery() {
        return query;
    }

    public String getGraph() {
        return graph;
    }

    public CustomQuestion(String query, String questionText, List<String> simpleModifiers, String graph) {
        this.query = query;
        this.questionText = questionText;
        this.modifiers = simpleModifiers;
        this.graph = graph;
    }
}
