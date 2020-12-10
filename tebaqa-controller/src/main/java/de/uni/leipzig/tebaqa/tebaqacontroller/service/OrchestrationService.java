package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.uni.leipzig.tebaqa.tebaqacommons.model.*;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.AnswerToQuestion;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.ResultsetBinding;
import de.uni.leipzig.tebaqa.tebaqacontroller.utils.SPARQLUtilities;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class OrchestrationService {

    private static final Logger LOGGER = Logger.getLogger(OrchestrationService.class);

    private final TemplateClassificationServiceConnector templateClassificationService;
    private final EntityLinkingServiceConnector entityLinkingService;
    private final QueryRankingServiceConnector queryRankingService;
    private final SemanticAnalysisHelper semanticAnalysisHelper;

    public OrchestrationService() throws IOException {
        this.templateClassificationService = new TemplateClassificationServiceConnector();
        this.entityLinkingService = new EntityLinkingServiceConnector();
        this.queryRankingService = new QueryRankingServiceConnector();
        this.semanticAnalysisHelper = new SemanticAnalysisHelper(Lang.EN);
//        this.semanticAnalysisHelper = new SemanticAnalysisHelper(new RestServiceConfiguration("http", "tebaqa.cs.upb.de", "8085"), Lang.EN);
    }

    public AnswerToQuestion answerQuestion(String question, Lang lang) throws JsonProcessingException {
        // 1. Template classification
        QueryTemplateResponseBean matchingQueryTemplates = templateClassificationService.getMatchingQueryTemplates(question, lang);
        printClassificationInfos(matchingQueryTemplates);

        boolean allTemplatesTried = false;
        if(matchingQueryTemplates.getTemplates().size() == 0) {
            LOGGER.warn("No query templates found!");
            matchingQueryTemplates = templateClassificationService.getAllQueryTemplates(question, lang);
            allTemplatesTried = true;
        }

        // 2. Entity linking
        EntityLinkingResponseBean entityLinkingResponse = entityLinkingService.extractEntities(question, lang);
        printLinkingInfos(entityLinkingResponse);

        // 3. Query ranking
        QueryRankingResponseBean queryRankingResponse = queryRankingService.generateQueries(question, lang, matchingQueryTemplates, entityLinkingResponse);
        printQueryRankingInfos(queryRankingResponse);

        Collection<RatedQuery> ratedQueries = queryRankingResponse.getGeneratedQueries();
        ResultsetBinding resultsetBinding = this.evaluateAndSelectBestQuery(question, ratedQueries);

        // Try all query templates if an answer is not yet found
        if(!allTemplatesTried && resultsetBinding.getResult().isEmpty()) {
            LOGGER.info("None of the queries worked, trying all templates");
            QueryTemplateResponseBean allQueryTemplates = templateClassificationService.getAllQueryTemplates(question, lang);
            printClassificationInfos(matchingQueryTemplates);

            // 3. Query ranking
            queryRankingResponse = queryRankingService.generateQueries(question, lang, allQueryTemplates, entityLinkingResponse);
            printQueryRankingInfos(queryRankingResponse);

            ratedQueries = queryRankingResponse.getGeneratedQueries();
            resultsetBinding = this.evaluateAndSelectBestQuery(question, ratedQueries);
        }

        LOGGER.info("Selected query: " + resultsetBinding.getQuery());
        return new AnswerToQuestion(resultsetBinding);

    }

    public ResultsetBinding evaluateAndSelectBestQuery(String question, Collection<RatedQuery> ratedQueries) {
        List<ResultsetBinding> queryResults = new ArrayList<>();
        for (RatedQuery ratedQuery : ratedQueries) {
            ResultsetBinding results = SPARQLUtilities.executeQuery(ratedQuery.getQuery());
            if (!results.getResult().isEmpty()) {
                results.setRating(ratedQuery.getRating());
                results.setRatedQuery(ratedQuery);
                queryResults.add(results);
            }
        }

//        ResultsetBinding ablationResult = this.ablation(question, queryResults);
//        if(ablationResult != null)
//            return ablationResult;

        final QuestionAnswerType expectedAnswerType = semanticAnalysisHelper.detectQuestionAnswerType(question);
        ResultsetBinding rsBinding = this.getBestAnswerNew(queryResults, expectedAnswerType, false);
        if (rsBinding.getResult().isEmpty())
            rsBinding = this.getBestAnswerNew(queryResults, expectedAnswerType, true);

        rsBinding.retrieveRedirects();
        return rsBinding;
    }

//    private ResultsetBinding ablation(String question, List<ResultsetBinding> queryResults) {
//        // ablation
//        if(ControllerPropertyUtils.ablationQueryRanking()) {
//            Set<String> goldenAnswers = AblationProvider.getGoldenAnswers(question);
//            ResultsetBinding best = null;
//
//            // handle differently for boolean answers
//            if(goldenAnswers.contains("true") || goldenAnswers.contains("false")) {
//                CustomQuestion enrichedQuestion = AblationProvider.getEnrichedQuestion(question);
//                if(enrichedQuestion != null) {
//                    EntityLinkingResponseBean linked = enrichedQuestion.getLinkedEntities();
//
//                    if (linked != null && (linked.getClassCandidates().size() + linked.getEntityCandidates().size() + linked.getPropertyCandidates().size() > 0)) {
//                        List<ResultsetBinding> filtered = queryResults.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType() != QuestionAnswerType.BOOLEAN_ANSWER_TYPE).collect(Collectors.toList());
//                        for (ResultsetBinding rs : filtered) {
//                            EntityLinkingResponseBean linkedForThis = AblationProvider.link(rs.getQuery(), question, false);
//                            boolean p = linkedForThis.getPropertyCandidates().containsAll(linked.getPropertyCandidates()) && linked.getPropertyCandidates().containsAll(linkedForThis.getPropertyCandidates());
//                            boolean c = linkedForThis.getClassCandidates().containsAll(linked.getClassCandidates()) && linked.getClassCandidates().containsAll(linkedForThis.getClassCandidates());
//                            boolean e = linkedForThis.getEntityCandidates().containsAll(linked.getEntityCandidates()) && linked.getEntityCandidates().containsAll(linkedForThis.getEntityCandidates());
//
//                            if (p & c & e) {
//                                LOGGER.info("Ablation QR found best boolean result");
//                                return rs;
//                            }
//                        }
//
//                    }
//                }
//
//            } else {
//                // Try to find exact matching results
//                for (ResultsetBinding result : queryResults) {
//                    if (goldenAnswers.containsAll(result.getResult()) && result.getResult().containsAll(goldenAnswers)) {
//                        best = result;
//                        break;
//                    }
//                }
//                if (best != null) {
//                    LOGGER.info("Ablation QR found best result");
//                    return best;
//                }
//
//                // If exact matching result is not found then select one which has maximum common results
//                Optional<ResultsetBinding> max = queryResults.stream().max(Comparator.comparingInt(o -> Sets.intersection(o.getResult(), goldenAnswers).size()));
//                if (max.isPresent()) {
//                    if (Sets.intersection(max.get().getResult(), goldenAnswers).size() > 0) {
//                        LOGGER.info("Ablation QR found best matching result");
//                        return max.get();
//                    }
//                }
//            }
//        }
//
//        return null;
//    }

    public ResultsetBinding getBestAnswerNew(List<ResultsetBinding> results, QuestionAnswerType expectedAnswerType, boolean forceResult) {
        Set<QuestionAnswerType> compatibleAnswerTypes = new HashSet<>();
        compatibleAnswerTypes.add(expectedAnswerType);
        if(forceResult){
            if(expectedAnswerType == QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE) {
                compatibleAnswerTypes.add(QuestionAnswerType.SINGLE_ANSWER);
            }
            if(expectedAnswerType == QuestionAnswerType.SINGLE_ANSWER) {
                compatibleAnswerTypes.add(QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE);
                compatibleAnswerTypes.add(QuestionAnswerType.NUMBER_ANSWER_TYPE);
                compatibleAnswerTypes.add(QuestionAnswerType.DATE_ANSWER_TYPE);
            }
        }

        long answersWithExpectedTypeCount = results.stream().filter(resultsetBinding -> compatibleAnswerTypes.contains(resultsetBinding.getAnswerType())).count();

        if (answersWithExpectedTypeCount > 0) {
            List<ResultsetBinding> matchingResults = results.stream().filter(resultsetBinding -> compatibleAnswerTypes.contains(resultsetBinding.getAnswerType())).collect(Collectors.toList());

            // If only one answer of expected type, then return it
            if (answersWithExpectedTypeCount == 1) {
                return matchingResults.get(0);
            }

//            // If more than one answer of expected type, then ranking
//            if (compatibleAnswerTypes.contains(QuestionAnswerType.NUMBER_ANSWER_TYPE)) {
//                List<ResultsetBinding> nonZeroCounts = matchingResults.stream().filter(resultsetBinding -> resultsetBinding.getResult().size() == 1 && resultsetBinding.getNumericalResultValue() != 0).collect(Collectors.toList());
//                if (nonZeroCounts.size() == 0)
//                    return matchingResults.get(0);
//
//                Map<Double, List<ResultsetBinding>> resultFrequencies = nonZeroCounts.stream().collect(Collectors.groupingBy(ResultsetBinding::getNumericalResultValue));
//                if (resultFrequencies.size() == 1) {
//                    return nonZeroCounts.get(0);
//                } else {
//                    // Use those numerical results which appear most frequently
//                    int maxFrequency = 0;
//                    double valueWithMaxFrequency = 0;
//                    for (double key : resultFrequencies.keySet()) {
//                        int frequency = resultFrequencies.get(key).size();
//                        if (frequency > maxFrequency) {
//                            maxFrequency = frequency;
//                            valueWithMaxFrequency = key;
//                        }
//                    }
//
//                    double finalValueWithMaxFrequency = valueWithMaxFrequency;
//                    return nonZeroCounts.stream().filter(resultsetBinding -> resultsetBinding.getNumericalResultValue() == finalValueWithMaxFrequency).findFirst().get();
//                }
//
//            } else if (compatibleAnswerTypes.contains(QuestionAnswerType.LIST_OF_RESOURCES_ANSWER_TYPE)) {
//                // Ranking criteria: More number of results mean less significant query
//                // Skip for now
////                matchingResults.forEach(resultsetBinding -> {
////                    resultsetBinding.setRating(resultsetBinding.getRating() / resultsetBinding.getResult().size());
////                });
//            } else {
//                // Ranking criteria: More number of results mean less significant query
//                // Skip for now
////                matchingResults.forEach(resultsetBinding -> {
////                    resultsetBinding.setRating(resultsetBinding.getRating() / resultsetBinding.getResult().size());
////                });
//            }
            Map<Double, List<ResultsetBinding>> resultByRating = matchingResults.stream().collect(Collectors.groupingBy(ResultsetBinding::getRating));
            Double maxRating = resultByRating.keySet().stream().max(Double::compareTo).get();
            List<ResultsetBinding> bestResults = resultByRating.get(maxRating);

            if(bestResults.size() == 1)
                return bestResults.get(0);
            else {
                // Select the query for which co occurrence length is maximum
                ResultsetBinding bestResult = new ResultsetBinding();
                int maxCoOccurrenceSum = 0;
                for (ResultsetBinding resultsetBinding : bestResults) {
                    // TODO can be weighted e.g. entity co occurrence lengths more important
                    int currentCoOccurrenceSum = 0;
                    RatedQuery ratedQuery = resultsetBinding.getRatedQuery();
                    currentCoOccurrenceSum += ratedQuery.getUsedEntities().stream().mapToInt(entity -> entity.getCoOccurrence().length()).sum();
                    currentCoOccurrenceSum += ratedQuery.getUsedProperties().stream().mapToInt(property -> property.getCoOccurrence().length()).sum();
                    currentCoOccurrenceSum += ratedQuery.getUsedClasses().stream().mapToInt(clazz -> clazz.getCoOccurrence().length()).sum();

                    if (currentCoOccurrenceSum > maxCoOccurrenceSum) {
                        bestResult = resultsetBinding;
                        maxCoOccurrenceSum = currentCoOccurrenceSum;
                    }
                }
                return bestResult;
            }

        } else {
            return new ResultsetBinding();
        }
    }

    public ResultsetBinding getBestAnswerOld(List<ResultsetBinding> results, QuestionAnswerType expectedAnswerType, boolean forceResult) {
        results.parallelStream().forEach(resultsetBinding -> {
            Map<String, String> bindings = resultsetBinding.getBindings();
            Double rating = 1.0;
            if (!forceResult && resultsetBinding.getAnswerType() != expectedAnswerType) {
                resultsetBinding.setRating(0.0);
            } else {
                if (rating > 0) {
                    if (resultsetBinding.getResult().size() >= 50) {
                        rating *= (1 / 3);
                    }
                    if (resultsetBinding.getAnswerType() != expectedAnswerType) {
                        rating *= 2;
                    }
                    if (resultsetBinding.getAnswerType() == QuestionAnswerType.BOOLEAN_ANSWER_TYPE && resultsetBinding.getResult().size() == 1 && resultsetBinding.getResult().stream().findFirst().get().equalsIgnoreCase("false")) {
                        rating *= (1 / 2);
                    }
                } else {
                    if (resultsetBinding.getResult().size() >= 50) {
                        rating *= 3;
                    }
                    if (resultsetBinding.getAnswerType() != expectedAnswerType) {
                        rating *= (1 / 2);
                    }

                    if (resultsetBinding.getAnswerType() == QuestionAnswerType.BOOLEAN_ANSWER_TYPE && resultsetBinding.getResult().size() == 1 && resultsetBinding.getResult().stream().findFirst().get().equalsIgnoreCase("false")) {
                        rating *= 2;
                    }
                }

                resultsetBinding.setRating(rating);
            }
        });

        Optional<ResultsetBinding> bestAnswerWithMatchingResultType = results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType() == expectedAnswerType).max(Comparator.comparingDouble(ResultsetBinding::getRating));
        List<ResultsetBinding>answersWithMatchingType=results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType() == expectedAnswerType).collect(Collectors.toList());
        double maxScore=-5;
        List<ResultsetBinding>bestAnswersWithMatchingType=new ArrayList<>();
        for(ResultsetBinding binding:answersWithMatchingType){
            if(binding.getRating()>maxScore){
                maxScore=binding.getRating();
                bestAnswersWithMatchingType.clear();
                bestAnswersWithMatchingType.add(binding);
            }
            else if (binding.getRating()== maxScore)bestAnswersWithMatchingType.add(binding);

        }
        if(bestAnswersWithMatchingType.size()>1){
            //prefer easier query

        }
        if (bestAnswersWithMatchingType.size()==1) {
            return bestAnswersWithMatchingType.get(0);
        } else {
            if (forceResult) {
                return results.stream()
                        .max(Comparator.comparingDouble(ResultsetBinding::getRating)).orElseGet(ResultsetBinding::new);
            } else {
                return results.stream()
                        .filter(resultsetBinding -> resultsetBinding.getRating() > 0)
                        .max(Comparator.comparingDouble(ResultsetBinding::getRating)).orElseGet(ResultsetBinding::new);
            }
        }
    }

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

//        LOGGER.debug("RAW JSON: ");
//        LOGGER.debug(JSONUtils.convertToJSONString(linkingResponseBean));

    }
}
