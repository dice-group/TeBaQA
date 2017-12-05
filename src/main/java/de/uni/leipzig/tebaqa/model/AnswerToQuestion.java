package de.uni.leipzig.tebaqa.model;

import java.util.Set;

public class AnswerToQuestion {
    private Set<String> answer;
    private Set<String> rdfEntities;

    public AnswerToQuestion(Set<String> answer, Set<String> rdfEntities) {
        this.answer = answer;
        this.rdfEntities = rdfEntities;
    }

    public Set<String> getAnswer() {
        return answer;
    }

    public Set<String> getRdfEntities() {
        return rdfEntities;
    }

    @Override
    public String toString() {
        return "AnswerToQuestion{" +
                "answer=" + answer +
                ", rdfEntities=" + rdfEntities +
                '}';
    }
}
