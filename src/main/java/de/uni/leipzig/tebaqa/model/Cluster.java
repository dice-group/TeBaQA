package de.uni.leipzig.tebaqa.model;

import org.aksw.qa.commons.datastructure.Question;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private String graph;
    private List<CustomQuestion> questions;

    public String getGraph() {
        return graph;
    }

    public Cluster(String graph) {
        this.graph = graph;
        questions = new ArrayList<>();
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
