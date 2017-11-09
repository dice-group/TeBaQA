package de.uni.leipzig.tebaqa.model;

import java.util.List;
import java.util.Map;

public class CustomQuestion {
    private List<String> modifiers;
    private String query;
    private String questionText;
    private String graph;
    private Map<String, List<String>> goldenAnswers;
    private Map<String, String> dependencySequencePosMap;

    public CustomQuestion(String query, String questionText, List<String> simpleModifiers, String graph, Map<String, List<String>> goldenAnswers) {
        this.query = query;
        this.questionText = questionText;
        this.modifiers = simpleModifiers;
        this.graph = graph;
        this.goldenAnswers = goldenAnswers;
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

    void setDependencySequencePosMap(Map<String, String> dependencySequencePosMap) {
        this.dependencySequencePosMap = dependencySequencePosMap;
    }

    public Map<String, List<String>> getGoldenAnswers() {
        return goldenAnswers;
    }

    @Override
    public String toString() {
        return "CustomQuestion{" +
                "modifiers=" + modifiers +
                ", query='" + query + '\'' +
                ", questionText='" + questionText + '\'' +
                ", graph='" + graph + '\'' +
                ", goldenAnswers=" + goldenAnswers +
                ", dependencySequencePosMap=" + dependencySequencePosMap +
                '}';
    }
}
