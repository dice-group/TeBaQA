package de.uni.leipzig.tebaqa.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class AnswerToQuestion {
    private Set<String> answer;
    private Set<String> rdfEntities;
    private String answerType;

    public AnswerToQuestion(Set<String> answer, Set<String> rdfEntities) {
        this.answer = answer;
        this.rdfEntities = rdfEntities;
        if (!answer.isEmpty() && answer.stream().allMatch(a -> a.startsWith("http://dbpedia.org/"))) {
            this.answerType = "uri";
            //NO uri but date, string or number
        } else {
            if (!answer.stream().allMatch(StringUtils::isNumeric)) {
                this.answerType = "number";
            } else {
                this.answerType = "literal";
            }
        }
    }

    public Set<String> getAnswer() {
        return answer;
    }

    public Set<String> getRdfEntities() {
        return rdfEntities;
    }

    public String getAnswerType() {
        return answerType;
    }

    @Override
    public String toString() {
        return "AnswerToQuestion{" +
                "answer=" + answer +
                ", rdfEntities=" + rdfEntities +
                ", answerType='" + answerType + '\'' +
                '}';
    }
}
