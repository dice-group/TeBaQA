package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.helper.SPARQLUtilities;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class AnswerToQuestion {
    private Set<String> answer;
    private Set<String> rdfEntities;
    private String answerType;

    public AnswerToQuestion(Set<String> answer, Set<String> rdfEntities) {
        this.answer = new HashSet<>();
        answer.forEach(s -> {
            if (s.startsWith("http://dbpedia.org/")) {
                String redirect = SPARQLUtilities.getRedirect(s);
                if (!redirect.isEmpty()) {
                    s = redirect;
                }
            }
            this.answer.add(s);
        });

        this.rdfEntities = rdfEntities;
        if (!this.answer.isEmpty() && this.answer.stream().allMatch(a -> a.startsWith("http://dbpedia.org/"))) {
            this.answerType = "uri";
        } else {
            //NO uri but date, string or number
            if (!this.answer.stream().allMatch(StringUtils::isNumeric)) {
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
