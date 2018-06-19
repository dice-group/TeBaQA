package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.model.AnswerToQuestion;
import de.uni.leipzig.tebaqa.model.ResultsetBinding;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ExtendedQALDAnswerTest {

    @Test
    public void testExtendedQALDAnswerGeneration() throws IOException {
        ResultsetBinding answer = new ResultsetBinding();
        answer.addResult("http://dbpedia.org/resource/Michelle_Obama");
        ExtendedQALDAnswer extendedQALDAnswer = new ExtendedQALDAnswer(new AnswerToQuestion(answer, new HashMap<>()));
        String result = extendedQALDAnswer.getResult();
        ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8.name()));
        ExtendedQALDJSONLoader.readJson(in);
    }

    @Test
    public void testExtendedQALDAnswerGenerationWithTwoAnswers() throws IOException {
        ResultsetBinding answer = new ResultsetBinding();
        answer.addResult("http://dbpedia.org/resource/Michelle_Obama");
        answer.addResult("http://dbpedia.org/resource/Michelle_Obama_2");
        ExtendedQALDAnswer extendedQALDAnswer = new ExtendedQALDAnswer(new AnswerToQuestion(answer, new HashMap<>()));
        String result = extendedQALDAnswer.getResult();
        ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8.name()));
        ExtendedQALDJSONLoader.readJson(in);
    }
}