package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.model.AnswerToQuestion;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.Set;

public class ExtendedQALDAnswer {

    private String result;

    public ExtendedQALDAnswer(String question, AnswerToQuestion answer, String lang) {
        JsonObjectBuilder resultBindings = Json.createObjectBuilder();
        Set<String> answers = answer.getAnswer();
        answers.forEach(a -> resultBindings.add("bindings", Json.createArrayBuilder().add(Json.createObjectBuilder()
                .add("x", Json.createObjectBuilder()
                        .add("type", answer.getAnswerType())
                        .add("value", a)))));
        JsonObjectBuilder questions = Json.createObjectBuilder()
                .add("questions", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("question", Json.createObjectBuilder()
                                .add("answers", Json.createObjectBuilder()
                                        .add("head", Json.createObjectBuilder()
                                                .add("vars", Json.createArrayBuilder().add("x")))
                                        .add("results", resultBindings).build().toString())
                        )));

        this.result = questions.build().toString();
    }

    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "ExtendedQALDAnswer{" +
                "result='" + result + '\'' +
                '}';
    }
}
