package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.model.AnswerToQuestion;
import org.jetbrains.annotations.NotNull;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.Set;

public class ExtendedQALDAnswer {

    private String result;

    ExtendedQALDAnswer(String question, AnswerToQuestion answer, String lang) {
        JsonArrayBuilder resultBindings = Json.createArrayBuilder();
        Set<String> answers = answer.getAnswer();
        answers.forEach(a -> {
            a = extractAnswerString(a);
            resultBindings.add(Json.createObjectBuilder()
                    .add("x", Json.createObjectBuilder()
                            .add("type", answer.getAnswerType())
                            .add("value", a)));
        });
        JsonObjectBuilder questions = Json.createObjectBuilder()
                .add("questions", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("question", Json.createObjectBuilder()
                                .add("answers", Json.createObjectBuilder()
                                        .add("head", Json.createObjectBuilder()
                                                .add("vars", Json.createArrayBuilder().add("x")))
                                        .add("results", Json.createObjectBuilder()
                                                .add("bindings", resultBindings)).build().toString()
                                ))));

        this.result = questions.build().toString();
    }

    @NotNull
    static String extractAnswerString(String a) {
        if (!a.isEmpty() && a.startsWith("'") && a.contains("'@")) {
            a = a.substring(0, a.lastIndexOf("'@") + 1);
        }
        if (!a.isEmpty() && a.endsWith("@en")) {
            a = a.substring(0, a.lastIndexOf("@"));
        }
        return a;
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
