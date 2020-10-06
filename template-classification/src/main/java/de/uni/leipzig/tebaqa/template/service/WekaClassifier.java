package de.uni.leipzig.tebaqa.template.service;

//import de.uni.leipzig.tebaqa.tebaqacommons.model.Cluster;
//import de.uni.leipzig.tebaqa.tebaqacommons.model.CustomQuestion;
//import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryIsomorphism;

import de.uni.leipzig.tebaqa.template.model.*;
import de.uni.leipzig.tebaqa.template.nlp.ISemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.template.nlp.SemanticAnalysisHelperEnglish;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class WekaClassifier {

    private static final Logger LOGGER = Logger.getLogger(WekaClassifier.class.getName());
    private final List<Dataset> trainDatasets = new ArrayList<>();
    private final List<Dataset> testDatasets = new ArrayList<>();
    private ISemanticAnalysisHelper semanticAnalysisHelper;
    private Map<String, QueryTemplateMapping> mappings;
    private Boolean evaluateWekaAlgorithms = false;
    private Boolean recalculateWekaModel = false;

    public WekaClassifier(List<Dataset> trainDatasets) {
        LOGGER.info("Configuring controller");
        this.semanticAnalysisHelper = new SemanticAnalysisHelperEnglish();
        this.trainDatasets.addAll(trainDatasets);
        LOGGER.info("Starting controller...");
        trainClassifier();
    }

    public WekaClassifier(List<Dataset> trainDatasets, List<Dataset> testDatasets) {
        LOGGER.info("Configuring controller");
        this.trainDatasets.addAll(trainDatasets);
        this.testDatasets.addAll(testDatasets);
        recalculateWekaModel = true;
        evaluateWekaAlgorithms = true;
        LOGGER.info("Starting controller...");
        trainClassifier();
    }

    private void trainClassifier() {
        List<HAWKQuestion> trainQuestions = new ArrayList<>();
        for (Dataset d : trainDatasets) {
            //Remove all trainQuestions without SPARQL query
            List<IQuestion> load = LoaderController.load(d);
            List<IQuestion> result = load.parallelStream()
                    .filter(question -> question.getSparqlQuery() != null)
                    .collect(Collectors.toList());
            trainQuestions.addAll(HAWKQuestionFactory.createInstances(result));
        }
        trainQuestions.addAll(HAWKQuestionFactory.createInstances(loadQuald9()));

        Map<String, String> trainQuestionsWithQuery = new HashMap<>();
        for (HAWKQuestion q : trainQuestions) {
            //only use unique trainQuestions in case multiple datasets are used
            String questionText = q.getLanguageToQuestion().get("en");
            if (!semanticAnalysisHelper.containsQuestionText(trainQuestionsWithQuery, questionText)) {
                trainQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
            }
        }

        //log.info("Generating ontology mapping...");
        //createOntologyMapping(trainQuestionsWithQuery);
        //log.info("Ontology Mapping: " + OntologyMappingProvider.getOntologyMapping());

        //List<CustomQuestion> customTrainQuestions;
        List<Cluster> customTrainQuestions;
        LOGGER.info("Building query clusters...");
        HashMap<String, Set<String>>[] commonPredicates = new HashMap[2];
        customTrainQuestions = transform(trainQuestionsWithQuery, commonPredicates);
        QueryBuilder queryBuilder = new QueryBuilder(customTrainQuestions, this.semanticAnalysisHelper);
        customTrainQuestions = queryBuilder.getQuestions();

        LOGGER.info("Extract query templates...");
        mappings = semanticAnalysisHelper.extractTemplates(customTrainQuestions, commonPredicates);

        LOGGER.info("Mappings were created...");
        List<String> graphs = new ArrayList<>();
        customTrainQuestions.forEach(customQuestion -> graphs.add(customQuestion.getGraph()));
        if (this.recalculateWekaModel) {
            LOGGER.info("Creating weka model...");
            Map<String, String> testQuestionsWithQuery = new HashMap<>();
            //only use unique trainQuestions in case multiple datasets are used
            /*for (HAWKQuestion q : trainQuestions) {
                String questionText = q.getLanguageToQuestion().get("de");
                if (!semanticAnalysisHelper.containsQuestionText(testQuestionsWithQuery, questionText)) {
                    testQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
                }
            }*/

            List<CustomQuestion> testSet = new ArrayList<>();
            List<CustomQuestion> trainSet = new ArrayList<>();
            customTrainQuestions.forEach(c -> trainSet.addAll(c.getQuestions()));
            customTrainQuestions.forEach(c -> testSet.addAll(c.getQuestions()));
            LOGGER.info("Instantiating ArffGenerator...");
            new ArffGenerator(graphs, trainSet, testSet, evaluateWekaAlgorithms);
            LOGGER.info("Instantiating ArffGenerator done!");

            LOGGER.info("Instantiating ArffGenerator done!");
        }

        ClassifierProvider.init(graphs);
//        testQuestions.parallelStream().forEach(q -> answerQuestion(graphs, q));
    }

    private List<Cluster> transform(Map<String, String> trainQuestionsWithQuery, HashMap<String, Set<String>>[] commonPredicates) {

        List<CustomQuestion> customTrainQuestions = new ArrayList<>();
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(trainQuestionsWithQuery, commonPredicates);

        return queryIsomorphism.getClusters();
    }
}
