package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import de.uni.leipzig.tebaqa.model.AnswerToQuestion;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;

import static de.uni.leipzig.tebaqa.helper.PipelineProvider.getQAPipeline;
import static de.uni.leipzig.tebaqa.spring.ExtendedQALDAnswer.extractAnswerString;

@RestController
public class QuestionAnsweringController {

    private static Logger log = Logger.getLogger(QuestionAnsweringController.class.getName());

    @RequestMapping(method = RequestMethod.POST, path = "/qa")
    public String answerQuestion(@RequestParam String query,
                                 @RequestParam(required = false, defaultValue = "en") String lang,
                                 HttpServletResponse response) {
        log.debug(String.format("Received POST request with: query='%s' and lang='%s'", query, lang));
        if (!query.isEmpty()) {
            PipelineController qaPipeline = getQAPipeline();
            String result;
            try {
                result = new ExtendedQALDAnswer(query, qaPipeline.answerQuestion(query), lang).getResult();
            } catch (Exception e) {
                result = new ExtendedQALDAnswer(query, new AnswerToQuestion(new HashSet<>(), new HashSet<>()), lang).getResult();
                log.error(String.format("Got Exception while answering='%s' with lang='%s'", query, lang), e);
            }
            log.debug("Answer:" + result);
            return result;
        } else {
            log.error("Received request with empty query parameter!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Parameter query cannot be empty!").toString();
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/qa-simple")
    public String answerQuestionSimple(@RequestParam String query,
                                       @RequestParam(required = false, defaultValue = "en") String lang,
                                       HttpServletResponse response) {
        log.debug(String.format("Received POST request with: query='%s' and lang='%s'", query, lang));
        if (!query.isEmpty()) {
            PipelineController qaPipeline = getQAPipeline();
            String result;
            try {
                AnswerToQuestion answer = qaPipeline.answerQuestion(query);
                JsonArrayBuilder resultArray = Json.createArrayBuilder();
                answer.getAnswer().forEach(a -> {
                    a = extractAnswerString(a);
                    resultArray.add(a);
                });
                result = Json.createObjectBuilder().add("answers", resultArray).build().toString();

            } catch (Exception e) {
                result = Json.createObjectBuilder().add("answers", Json.createArrayBuilder()).build().toString();
                log.error(String.format("Got Exception while answering='%s' with lang='%s'", query, lang), e);
            }
            log.debug("Answer:" + result);
            return result;
        } else {
            log.error("Received request with empty query parameter!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Parameter query cannot be empty!").toString();
        }
    }
}
