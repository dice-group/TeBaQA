package de.uni.leipzig.tebaqa.template.model;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private final String graph;
    private final List<CustomQuestion> questions;

    public Cluster(String graph) {
        this.graph = graph;
        questions = new ArrayList<>();
    }

    public String getGraph() {
        return graph;
    }

    public Integer size() {
        return questions.size();
    }

    public void addQuestion(CustomQuestion question) {
        this.questions.add(question);
    }

    public List<CustomQuestion> getQuestions() {
        return questions;
    }
}
