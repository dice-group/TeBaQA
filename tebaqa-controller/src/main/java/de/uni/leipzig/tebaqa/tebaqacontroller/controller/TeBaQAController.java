package de.uni.leipzig.tebaqa.tebaqacontroller.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryRankingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryTemplateResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.util.JSONUtils;
import de.uni.leipzig.tebaqa.tebaqacontroller.service.EntityLinkingServiceConnector;
import de.uni.leipzig.tebaqa.tebaqacontroller.service.QueryRankingServiceConnector;
import de.uni.leipzig.tebaqa.tebaqacontroller.service.TemplateClassificationServiceConnector;
import de.uni.leipzig.tebaqa.tebaqacontroller.utils.ControllerPropertyUtils;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
public class TeBaQAController {

    private static final Logger LOGGER = Logger.getLogger(TeBaQAController.class.getName());

    private final TemplateClassificationServiceConnector templateClassificationService = new TemplateClassificationServiceConnector();
    private final EntityLinkingServiceConnector entityLinkingService = new EntityLinkingServiceConnector();
    private final QueryRankingServiceConnector queryRankingService = new QueryRankingServiceConnector();

    @RequestMapping(method = RequestMethod.POST, path = "/qa-simple")
    public String answerQuestionSimple(@RequestParam String question,
                                       @RequestParam(required = false, defaultValue = "en") String lang,
                                       HttpServletResponse response) throws JsonProcessingException {
        LOGGER.info(String.format("/qa-simple received POST request with: question='%s' and lang='%s'", question, lang));
        if (!question.isEmpty() && isValidQuestion(question)) {
            // 1. Template classification
            QueryTemplateResponseBean matchingQueryTemplates = templateClassificationService.getMatchingQueryTemplates(question, lang);
            printClassificationInfos(matchingQueryTemplates);

            // 2. Entity linking
            EntityLinkingResponseBean entityLinkingResponse = entityLinkingService.extractEntities(question, lang);
            printLinkingInfos(entityLinkingResponse);

            // 3. Query ranking
            QueryRankingResponseBean queryRankingResponse = queryRankingService.generateQueries(question, lang, matchingQueryTemplates, entityLinkingResponse);
            printQueryRankingInfos(queryRankingResponse);

//            PipelineController qaPipeline = getQAPipeline();
//            String result;
//            try {
//                AnswerToQuestion answer = qaPipeline.answerLimboQuestion(question);
//                JsonArrayBuilder resultArray = Json.createArrayBuilder();
//                answer.getAnswer().forEach(a -> resultArray.add(extractAnswerString(a)));
//                result = Json.createObjectBuilder()
//                        .add("answers", resultArray)
//                        .add("sparql", answer.getSparqlQuery())
//                        .build().toString();
//
//            } catch (Exception e) {
//                result = Json.createObjectBuilder().add("answers", Json.createArrayBuilder()).build().toString();
//                LOGGER.error(String.format("Got Exception while answering='%s' with lang='%s'", question, lang), e);
//            }
//            LOGGER.info("Answer: " + result);
//            return result;
            return ResponseEntity.status(HttpStatus.OK).body(JSONUtils.convertToJSONString(queryRankingResponse.getGeneratedQueries())).toString();

        } else {
            LOGGER.error("Received request with empty question parameter!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Parameter question cannot be empty!").toString();
        }
    }

    private void printQueryRankingInfos(QueryRankingResponseBean queryRankingResponse) {
        LOGGER.info("Queries generated: " + queryRankingResponse.getGeneratedQueries().size());
        queryRankingResponse.getGeneratedQueries().forEach(LOGGER::debug);
    }

    private static void printClassificationInfos(QueryTemplateResponseBean matchingQueryTemplates) {
        LOGGER.info("Templates found: " + matchingQueryTemplates.getTemplates().size());
        matchingQueryTemplates.getTemplates().forEach(LOGGER::debug);
    }

    private static void printLinkingInfos(EntityLinkingResponseBean linkingResponseBean) throws JsonProcessingException {
        LOGGER.info("Classes found: " + linkingResponseBean.getClassCandidates().size());
        linkingResponseBean.getClassCandidates().forEach(s -> LOGGER.debug(s.getCoOccurrence() + " --> " + s.getUri()));

        LOGGER.info("Properties found: " + linkingResponseBean.getPropertyCandidates().size());
        linkingResponseBean.getPropertyCandidates().forEach(s -> LOGGER.debug(s.getCoOccurrence() + " --> " + s.getUri()));

        LOGGER.info("Entities found: " + linkingResponseBean.getEntityCandidates().size());
        linkingResponseBean.getEntityCandidates().forEach(s -> LOGGER.debug(s.getCoOccurrence() + " --> " + s.getUri()));

        LOGGER.info("RAW JSON: ");
        LOGGER.info(JSONUtils.convertToJSONString(linkingResponseBean));

    }


    //    @RequestMapping(method = RequestMethod.POST, path = "/qa")
//    public String answerQuestion(@RequestParam String query,
//                                 @RequestParam(required = false, defaultValue = "en") String lang,
//                                 HttpServletResponse response) {
//        LOGGER.info(String.format("/qa received POST request with: query='%s' and lang='%s'", query, lang));
//        if (!query.isEmpty() && isValidQuestion(query)) {
//            PipelineController qaPipeline = getQAPipeline();
//            String result;
//            try {
//                result = new ExtendedQALDAnswer(qaPipeline.answerLimboQuestion(query)).getResult();
//            } catch (Exception e) {
//                result = new ExtendedQALDAnswer(new AnswerToQuestion(new ResultsetBinding(), new HashMap<>())).getResult();
//                LOGGER.error(String.format("Got Exception while answering='%s' with lang='%s'", query, lang), e);
//            }
//            LOGGER.info("Answer: " + result);
//            return result;
//        } else {
//            LOGGER.error("Received request with empty query parameter!");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Parameter query cannot be empty!").toString();
//        }
//    }
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
