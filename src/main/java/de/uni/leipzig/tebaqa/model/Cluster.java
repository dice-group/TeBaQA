package de.uni.leipzig.tebaqa.model;

import org.aksw.qa.commons.datastructure.Question;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    public String getGraph() {
        return graph;
    }

    private String graph;
    private List<Question> questions;

    public Cluster(String graph) {
        this.graph = graph;
        questions = new ArrayList<>();
    }

    public Integer size() {
        return questions.size();
    }

    public void addQuestion(Question question) {
        this.questions.add(question);
    }

    public List<Question> getQuestions() {
        return questions;
    }
}
