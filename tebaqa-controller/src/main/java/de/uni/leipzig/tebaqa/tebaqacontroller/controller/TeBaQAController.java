package de.uni.leipzig.tebaqa.tebaqacontroller.controller;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.AnswerToQuestion;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.ExtendedQALDAnswer;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.ResultsetBinding;
import de.uni.leipzig.tebaqa.tebaqacontroller.service.OrchestrationService;
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
import java.util.HashMap;

@RestController
public class TeBaQAController {

    private static final Logger LOGGER = Logger.getLogger(TeBaQAController.class.getName());
    private static final OrchestrationService qaService = new OrchestrationService();

    @RequestMapping(method = RequestMethod.POST, path = "/qa-simple")
    public String answerQuestionSimple(@RequestParam String query,
                                       @RequestParam(required = false, defaultValue = "en") String lang,
                                       HttpServletResponse response) {
        LOGGER.info(String.format("/qa-simple received POST request with: question='%s' and lang='%s'", query, lang));

        Lang language = Lang.getForCode(lang);
        if (!query.isEmpty() && isValidQuestion(query) && language != null) {
            String result;
            try {
                AnswerToQuestion answer = qaService.answerQuestion(query, language);
                JsonArrayBuilder resultArray = Json.createArrayBuilder();
                answer.getAnswer().forEach(a -> resultArray.add(ExtendedQALDAnswer.extractAnswerString(a)));
                result = Json.createObjectBuilder()
                        .add("answers", resultArray)
                        .add("sparql", answer.getSparqlQuery())
                        .build().toString();

            } catch (Exception e) {
                result = Json.createObjectBuilder().add("answers", Json.createArrayBuilder()).build().toString();
                LOGGER.error(String.format("Got Exception while answering='%s' with lang='%s'", query, lang), e);
            }
            LOGGER.info("Answer: " + result);
            return result;

        } else {
            LOGGER.error("Received request with empty question parameter!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Parameter question cannot be empty!").toString();
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/qa")
    public String answerQuestion(@RequestParam String query,
                                 @RequestParam(required = false, defaultValue = "en") String lang,
                                 HttpServletResponse response) {
        LOGGER.info(String.format("/qa received POST request with: question='%s' and lang='%s'", query, lang));

        Lang language = Lang.getForCode(lang);
        if (!query.isEmpty() && isValidQuestion(query) && language != null) {
            String result;
            try {
                AnswerToQuestion answer = qaService.answerQuestion(query, language);
                result = new ExtendedQALDAnswer(answer).getResult();
            } catch (Exception e) {
                result = new ExtendedQALDAnswer(new AnswerToQuestion(new ResultsetBinding(), new HashMap<>())).getResult();
                LOGGER.error(String.format("Got Exception while answering='%s' with lang='%s'", query, lang), e);
            }
            LOGGER.info("Answer: " + result);
            return result;

        } else {
            LOGGER.error("Received request with empty question parameter!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Parameter question cannot be empty!").toString();
        }

    }
//
//    @RequestMapping(method = RequestMethod.GET, path = "/qa")
//    public String answerQuestion2(@RequestParam String query,
//                                  @RequestParam(required = false, defaultValue = "en") String lang,
//                                  HttpServletResponse response) {
//        return this.answerQuestion(query, lang, response);
//    }
//    @RequestMapping(method = RequestMethod.POST, path = "/keyword")
//    public String keywordQuery(@RequestParam String query,
//                               @RequestParam(required = false, defaultValue = "en") String lang,
//                               @RequestParam(required = false, defaultValue = "") String type,
//                               @RequestParam(required = false, defaultValue = "") String property,
//                               @RequestParam(required = false, defaultValue = "") String connect,
//                               @RequestParam(required = false, defaultValue = "") String searchIn,
//                               HttpServletResponse response) {
//        KeyWordController pipeline = getKeyWordPipeline();
//        String result;
//        try {
//            Optional<String> search = Optional.empty();
//            if (!searchIn.equals("all")) search = Optional.of(searchIn);
//            Optional<String> connResoure = Optional.empty();
//            if (!connect.equals("")) connResoure = Optional.of(connect);
//            Optional<String> connProp = Optional.empty();
//            if (!property.equals("")) connProp = Optional.of(property);
//            Optional<String> connClass = Optional.empty();
//            if (!type.equals("")) connClass = Optional.of(type);
//            AnswerToQuestion answer = new AnswerToQuestion(pipeline.searchByKeywords(query, search, connClass, connProp, connResoure));
//            JsonArrayBuilder resultArray = Json.createArrayBuilder();
//            answer.getAnswer().forEach(a -> resultArray.add(extractAnswerString(a)));
//            result = Json.createObjectBuilder()
//                    .add("answers", resultArray)
//                    .add("sparql", answer.getSparqlQuery())
//                    .build().toString();
//
//        } catch (Exception e) {
//            result = Json.createObjectBuilder().add("answers", Json.createArrayBuilder()).build().toString();
//            LOGGER.error(String.format("Got Exception while answering='%s' with lang='%s'", query, lang), e);
//        }
//        LOGGER.info("Answer: " + result);
//        return result;
//    }


    private boolean isValidQuestion(String q) {
        return q.trim().length() > 0;
    }
}
