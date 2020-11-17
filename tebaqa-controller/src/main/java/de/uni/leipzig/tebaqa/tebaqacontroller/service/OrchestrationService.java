package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.uni.leipzig.tebaqa.tebaqacommons.model.*;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.SemanticAnalysisHelperEnglish;
import de.uni.leipzig.tebaqa.tebaqacommons.util.JSONUtils;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.AnswerToQuestion;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.ResultsetBinding;
import de.uni.leipzig.tebaqa.tebaqacontroller.utils.SPARQLUtilities;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class OrchestrationService {

    private static final Logger LOGGER = Logger.getLogger(OrchestrationService.class);

    private final TemplateClassificationServiceConnector templateClassificationService;
    private final EntityLinkingServiceConnector entityLinkingService;
    private final QueryRankingServiceConnector queryRankingService;
    private final SemanticAnalysisHelper semanticAnalysisHelper;

    public OrchestrationService() {
        this.templateClassificationService = new TemplateClassificationServiceConnector();
        this.entityLinkingService = new EntityLinkingServiceConnector();
        this.queryRankingService = new QueryRankingServiceConnector();
        this.semanticAnalysisHelper = new SemanticAnalysisHelperEnglish();
    }

    public AnswerToQuestion answerQuestion(String question, Lang lang) throws JsonProcessingException {
        // 1. Template classification
        QueryTemplateResponseBean matchingQueryTemplates = templateClassificationService.getMatchingQueryTemplates(question, lang);
        printClassificationInfos(matchingQueryTemplates);

        // 2. Entity linking
        EntityLinkingResponseBean entityLinkingResponse = entityLinkingService.extractEntities(question, lang);
        printLinkingInfos(entityLinkingResponse);

        // 3. Query ranking
        QueryRankingResponseBean queryRankingResponse = queryRankingService.generateQueries(question, lang, matchingQueryTemplates, entityLinkingResponse);
        printQueryRankingInfos(queryRankingResponse);

        Collection<RatedQuery> ratedQueries = queryRankingResponse.getGeneratedQueries();

        //If the template from the predicted graph won't find suitable templates, try all other templates
        List<ResultsetBinding> queryResults = new ArrayList<>();
        for (RatedQuery ratedQuery : ratedQueries) {
            ResultsetBinding results = SPARQLUtilities.executeQuery(ratedQuery.getQuery());
            if (!results.getResult().isEmpty()) {
                results.setRating(ratedQuery.getRating());
                queryResults.add(results);
            }
        }

        final QuestionAnswerType expectedAnswerType = semanticAnalysisHelper.detectQuestionAnswerType(question);
        ResultsetBinding rsBinding = this.getBestAnswer(queryResults, expectedAnswerType, false);

        rsBinding.retrieveRedirects();
        return new AnswerToQuestion(rsBinding);
    }

    public ResultsetBinding getBestAnswer(List<ResultsetBinding> results, QuestionAnswerType expectedAnswerType, boolean forceResult) {
        long answersWithExpectedTypeCount = results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType() == expectedAnswerType).count();

        if (answersWithExpectedTypeCount > 0) {
            List<ResultsetBinding> matchingResults = results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType() == expectedAnswerType).collect(Collectors.toList());

            // If only one answer of expected type, then return it
            if (answersWithExpectedTypeCount == 1) {
                return matchingResults.get(0);
            }

            // If more than one answer of expected type, then ranking
            if (QuestionAnswerType.NUMBER_ANSWER_TYPE == expectedAnswerType) {
                List<ResultsetBinding> nonZeroCounts = matchingResults.stream().filter(resultsetBinding -> resultsetBinding.getResult().size() == 1 && resultsetBinding.getNumericalResultValue() != 0).collect(Collectors.toList());
                if (nonZeroCounts.size() == 0)
                    return matchingResults.get(0);

                Map<Double, List<ResultsetBinding>> resultFrequencies = nonZeroCounts.stream().collect(Collectors.groupingBy(ResultsetBinding::getNumericalResultValue));
                if (resultFrequencies.size() == 1) {
                    return nonZeroCounts.get(0);
                } else {
                    // Use those numerical results which appear most frequently
                    int maxFrequency = 0;
                    double valueWithMaxFrequency = 0;
                    for (double key : resultFrequencies.keySet()) {
                        int frequency = resultFrequencies.get(key).size();
                        if (frequency > maxFrequency) {
                            maxFrequency = frequency;
                            valueWithMaxFrequency = key;
                        }
                    }

                    double finalValueWithMaxFrequency = valueWithMaxFrequency;
                    return nonZeroCounts.stream().filter(resultsetBinding -> resultsetBinding.getNumericalResultValue() == finalValueWithMaxFrequency).findFirst().get();
                }

            } else if (QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE.equals(expectedAnswerType)) {
                // Ranking criteria: More number of results mean less significant query
                matchingResults.forEach(resultsetBinding -> {
                    resultsetBinding.setRating(resultsetBinding.getRating() / resultsetBinding.getResult().size());
                });
            } else {
                // Ranking criteria: More number of results mean less significant query
                matchingResults.forEach(resultsetBinding -> {
                    resultsetBinding.setRating(resultsetBinding.getRating() / resultsetBinding.getResult().size());
                });
            }
            return matchingResults.stream().max(Comparator.comparingDouble(ResultsetBinding::getRating)).get();

        } else {
            return new ResultsetBinding();
        }

    }

//    private ResultsetBinding getBestAnswer(List<ResultsetBinding> results, QuestionAnswerType expectedAnswerType, boolean forceResult) {
//        results.parallelStream().forEach(resultsetBinding -> {
//            Map<String, String> bindings = resultsetBinding.getBindings();
//            double rating = 1.0;
//            if (!forceResult && !resultsetBinding.getAnswerType().equals(expectedAnswerType)) {
//                resultsetBinding.setRating(0.0);
//            } else {
//                if (rating > 0) {
//                    if (resultsetBinding.getResult().size() >= 50) {
//                        rating *= (1.0 / 3);
//                    }
//                    if (!resultsetBinding.getAnswerType().equals(expectedAnswerType)) {
//                        rating *= 2;
//                    }
//                    if (expectedAnswerType.equals(QuestionAnswerType.NUMBER_ANSWER_TYPE) && results.size() == 1)
//                        rating *= 3;
//                    if (resultsetBinding.getAnswerType().equals(QuestionAnswerType.BOOLEAN_ANSWER_TYPE) && resultsetBinding.getResult().size() == 1 && resultsetBinding.getResult().stream().findFirst().get().equalsIgnoreCase("false")) {
//                        rating *= (1.0 / 2);
//                    }
//                } else {
//                    if (resultsetBinding.getResult().size() >= 50) {
//                        rating *= 3;
//                    }
//                    if (expectedAnswerType.equals(QuestionAnswerType.NUMBER_ANSWER_TYPE) && results.size() == 1)
//                        rating *= 1.0 / 3;
//                    if (resultsetBinding.getAnswerType().equals(expectedAnswerType)) {
//                        rating *= (1.0 / 2);
//                    }
//
//                    if (resultsetBinding.getAnswerType().equals(QuestionAnswerType.BOOLEAN_ANSWER_TYPE) && resultsetBinding.getResult().size() == 1 && resultsetBinding.getResult().stream().findFirst().get().equalsIgnoreCase("false")) {
//                        rating *= 2;
//                    }
//                }
//
//                resultsetBinding.setRating(rating);
//            }
//        });
//
//        Optional<ResultsetBinding> bestAnswerWithMatchingResultType = results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType().equals(expectedAnswerType)).max(Comparator.comparingDouble(ResultsetBinding::getRating));
//        List<ResultsetBinding> answersWithMatchingType = results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType().equals(expectedAnswerType)).collect(Collectors.toList());
//        double maxScore = -5;
//        List<ResultsetBinding> bestAnswersWithMatchingType = new ArrayList<>();
//        for (ResultsetBinding binding : answersWithMatchingType) {
//            if (binding.getRating() > maxScore) {
//                maxScore = binding.getRating();
//                bestAnswersWithMatchingType.clear();
//                bestAnswersWithMatchingType.add(binding);
//            } else if (binding.getRating() == maxScore) bestAnswersWithMatchingType.add(binding);
//
//        }
//        if (bestAnswersWithMatchingType.size() > 1) {
//            //prefer easier query
//
//        }
//        if (bestAnswersWithMatchingType.size() == 1) {
//            return bestAnswersWithMatchingType.get(0);
//        } else {
//            if (forceResult) {
//                return results.stream()
//                        .max(Comparator.comparingDouble(ResultsetBinding::getRating)).orElseGet(ResultsetBinding::new);
//            } else {
//                return results.stream()
//                        .filter(resultsetBinding -> resultsetBinding.getRating() > 0)
//                        .max(Comparator.comparingDouble(ResultsetBinding::getRating)).orElseGet(ResultsetBinding::new);
//            }
//        }
//    }

    private void printQueryRankingInfos(QueryRankingResponseBean queryRankingResponse) {
        LOGGER.info("Queries generated: " + queryRankingResponse.getGeneratedQueries().size());
        queryRankingResponse.getGeneratedQueries().forEach(LOGGER::debug);
    }

    private static void printClassificationInfos(QueryTemplateResponseBean matchingQueryTemplates) {
        LOGGER.info("Templates found: " + matchingQueryTemplates.getTemplates().size());
        matchingQueryTemplates.getTemplates().forEach(LOGGER::info);
    }

    private static void printLinkingInfos(EntityLinkingResponseBean linkingResponseBean) throws JsonProcessingException {
        LOGGER.info("Classes found: " + linkingResponseBean.getClassCandidates().size());
        linkingResponseBean.getClassCandidates().forEach(s -> LOGGER.debug(s.getCoOccurrence() + " --> " + s.getUri()));

        LOGGER.info("Properties found: " + linkingResponseBean.getPropertyCandidates().size());
        linkingResponseBean.getPropertyCandidates().forEach(s -> LOGGER.debug(s.getCoOccurrence() + " --> " + s.getUri()));

        LOGGER.info("Entities found: " + linkingResponseBean.getEntityCandidates().size());
        linkingResponseBean.getEntityCandidates().forEach(s -> LOGGER.debug(s.getCoOccurrence() + " --> " + s.getUri()));

        LOGGER.debug("RAW JSON: ");
        LOGGER.debug(JSONUtils.convertToJSONString(linkingResponseBean));

    }
}
