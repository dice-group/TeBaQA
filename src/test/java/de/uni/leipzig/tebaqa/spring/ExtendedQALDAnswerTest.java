package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.model.AnswerToQuestion;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class ExtendedQALDAnswerTest {

    @Test
    public void testExtendedQALDAnswerGeneration() throws IOException {
        Set<String> answer = new HashSet<>();
        answer.add("http://dbpedia.org/resource/Michelle_Obama");
        ExtendedQALDAnswer extendedQALDAnswer = new ExtendedQALDAnswer("Who is the spouse of Barack Obama?",
                new AnswerToQuestion(answer, new HashSet<>()), "en");
        String result = extendedQALDAnswer.getResult();
        ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8.name()));
        ExtendedQALDJSONLoader.readJson(in);
    }

    @Test
    public void testExtendedQALDAnswerGenerationWithTwoAnswers() throws IOException {
        Set<String> answer = new HashSet<>();
        answer.add("http://dbpedia.org/resource/Michelle_Obama");
        answer.add("http://dbpedia.org/resource/Michelle_Obama_2");
        ExtendedQALDAnswer extendedQALDAnswer = new ExtendedQALDAnswer("Who is the spouse of Barack Obama?",
                new AnswerToQuestion(answer, new HashSet<>()), "en");
        String result = extendedQALDAnswer.getResult();
        ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8.name()));
        ExtendedQALDJSONLoader.readJson(in);
    }
}