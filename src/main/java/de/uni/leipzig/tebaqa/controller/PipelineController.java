package de.uni.leipzig.tebaqa.controller;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.helper.DBpediaPropertiesProvider;
import de.uni.leipzig.tebaqa.helper.NTripleParser;
import de.uni.leipzig.tebaqa.helper.OntologyMappingProvider;
import de.uni.leipzig.tebaqa.helper.PosTransformation;
import de.uni.leipzig.tebaqa.helper.QueryMappingFactory;
import de.uni.leipzig.tebaqa.helper.SPARQLUtilities;
import de.uni.leipzig.tebaqa.helper.TextUtilities;
import de.uni.leipzig.tebaqa.helper.Utilities;
import de.uni.leipzig.tebaqa.model.AnswerToQuestion;
import de.uni.leipzig.tebaqa.model.Cluster;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryBuilder;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import de.uni.leipzig.tebaqa.model.WordNetWrapper;
import edu.cmu.lti.jawjaw.pobj.POS;
import joptsimple.internal.Strings;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.jena.query.QueryParseException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.uni.leipzig.tebaqa.helper.QueryMappingFactory.NON_WORD_CHARACTERS_REGEX;
import static java.util.Collections.emptyList;


public class PipelineController {

    private static Logger log = Logger.getLogger(PipelineController.class.getName());

    private static SemanticAnalysisHelper semanticAnalysisHelper;
    private List<Dataset> trainDatasets = new ArrayList<>();
    private Map<String, QueryTemplateMapping> mappings;
    private HashSet<String> graphs = new HashSet<>();
    private Boolean evaluateWekaAlgorithms = false;


    public PipelineController(List<Dataset> trainDatasets) {
        log.info("Configuring controller");
        semanticAnalysisHelper = new SemanticAnalysisHelper();
        trainDatasets.forEach(this::addTrainDataset);
        log.info("Starting controller...");
        run();
    }

    private void run() {
        List<HAWKQuestion> trainQuestions = new ArrayList<>();
        for (Dataset d : trainDatasets) {
            //Remove all trainQuestions without SPARQL query
            List<IQuestion> load = LoaderController.load(d);
            List<IQuestion> result = load.parallelStream()
                    .filter(question -> question.getSparqlQuery() != null)
                    .collect(Collectors.toList());
            trainQuestions.addAll(HAWKQuestionFactory.createInstances(result));
        }
        Map<String, String> trainQuestionsWithQuery = new HashMap<>();
        for (HAWKQuestion q : trainQuestions) {
            //only use unique trainQuestions in case multiple datasets are used
            String questionText = q.getLanguageToQuestion().get("en");
            if (!semanticAnalysisHelper.containsQuestionText(trainQuestionsWithQuery, questionText)) {
                trainQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
            }
        }

        log.info("Generating ontology mapping...");
        createOntologyMapping(trainQuestionsWithQuery);
        log.debug("Ontology Mapping: " + OntologyMappingProvider.getOntologyMapping());

        log.info("Getting DBpedia properties from SPARQL endpoint...");
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();

        log.info("Parsing DBpedia n-triples from file...");
        Set<RDFNode> ontologyNodes = NTripleParser.getNodes();

        List<CustomQuestion> customTrainQuestions;

        log.info("Building query clusters...");
        customTrainQuestions = transform(trainQuestionsWithQuery);
        QueryBuilder queryBuilder = new QueryBuilder(customTrainQuestions, semanticAnalysisHelper);
        customTrainQuestions = queryBuilder.getQuestions();

        log.info("Extract query templates...");
        mappings = semanticAnalysisHelper.extractTemplates(customTrainQuestions, Lists.newArrayList(ontologyNodes), dBpediaProperties);

        log.info("Creating weka model...");
        Map<String, String> testQuestionsWithQuery = new HashMap<>();
        for (HAWKQuestion q : trainQuestions) {
            //only use unique trainQuestions in case multiple datasets are used
            String questionText = q.getLanguageToQuestion().get("en");
            if (!semanticAnalysisHelper.containsQuestionText(testQuestionsWithQuery, questionText)) {
                testQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
            }
        }
        new ArffGenerator(customTrainQuestions, transform(testQuestionsWithQuery), evaluateWekaAlgorithms);

        graphs = new HashSet<>();
        customTrainQuestions.forEach(customQuestion -> graphs.add(customQuestion.getGraph()));

//        testQuestions.parallelStream().forEach(q -> answerQuestion(graphs, q));
    }

    private List<CustomQuestion> transform(Map<String, String> trainQuestionsWithQuery) {
        List<CustomQuestion> customTrainQuestions = new ArrayList<>();
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(trainQuestionsWithQuery);
        List<Cluster> clusters = queryIsomorphism.getClusters();
        for (Cluster cluster : clusters) {
            String graph = cluster.getGraph();
            List<Question> questionList = cluster.getQuestions();
            for (Question question : questionList) {
                String questionText = question.getLanguageToQuestion().get("en");
                String sparqlQuery = question.getSparqlQuery();
                List<String> simpleModifiers = getSimpleModifiers(sparqlQuery);
                Map<String, List<String>> goldenAnswers = new HashMap<>();
                List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(sparqlQuery);
                List<String> resultSet = new ArrayList<>();
                sparqlResultSets.forEach(sparqlResultSet -> resultSet.addAll(sparqlResultSet.getResultSet()));
                goldenAnswers.put(sparqlQuery, resultSet);
                customTrainQuestions.add(new CustomQuestion(sparqlQuery, questionText, simpleModifiers, graph, goldenAnswers));
//                semanticAnalysisHelper.annotate(questionText);
            }
        }
        return customTrainQuestions;
    }

    private void createOntologyMapping(Map<String, String> questionsWithQuery) {
        Map<String, Set<String>> lemmaOntologyMapping = new HashMap<>();
        questionsWithQuery.forEach((sparqlQuery, questionText) -> {
            WordNetWrapper wordNetWrapper = new WordNetWrapper();
            ArrayList<String> words = Lists.newArrayList(questionText.split(NON_WORD_CHARACTERS_REGEX));
            Matcher matcher = Utilities.BETWEEN_LACE_BRACES.matcher(SPARQLUtilities.resolveNamespaces(sparqlQuery));
            while (matcher.find()) {
                String entity = matcher.group().replace("<", "").replace(">", "");
                if (!entity.startsWith("http://dbpedia.org/resource/")) {
                    for (String word : words) {
                        Map<String, String> lemmas = SemanticAnalysisHelper.getLemmas(word);
                        String lemma;
                        if (lemmas.size() == 1 && lemmas.containsKey(word)) {
                            lemma = lemmas.get(word);
                        } else {
                            lemma = word;
                        }
                        String[] entityParts = entity.split("/");
                        String entityName = entityParts[entityParts.length - 1];
                        if (word.equals(entityName)) {
                            //Equal ontologies like parent -> http://dbpedia.org/ontology/parent are detected already
                            continue;
                        }
                        String wordPosString = SemanticAnalysisHelper.getPOS(lemma).getOrDefault(lemma, "");
                        POS currentWordPOS = PosTransformation.transform(wordPosString);
                        String posStringOfEntity = SemanticAnalysisHelper.getPOS(entityName).getOrDefault(entityName, "");
                        POS entityPOS = PosTransformation.transform(posStringOfEntity);
                        Double similarity;
                        if (entityPOS == null || currentWordPOS == null) {
                            continue;

                        }
                        if (entityName.length() > 1 && SemanticAnalysisHelper.countUpperCase(entityName.substring(1, entityName.length() - 1)) > 0) {
                            similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord(entityName, lemma);
                        } else {
                            similarity = wordNetWrapper.semanticWordSimilarity(lemma, entityName);
                        }

                        if (similarity.compareTo(1.0) > 0) {
                            Set<String> entities;
                            if (lemmaOntologyMapping.containsKey(lemma)) {
                                entities = lemmaOntologyMapping.get(lemma);
                            } else {
                                entities = new HashSet<>();
                            }
                            entities.add(entity);
                            lemmaOntologyMapping.put(lemma, entities);
                        }
                    }
                }
            }
        });
        OntologyMappingProvider.setOntologyMapping(lemmaOntologyMapping);
    }

    public AnswerToQuestion answerQuestion(String question) {
        return answerQuestion(question, graphs);
    }

    private void answerQuestion(HashSet<String> graphs, HAWKQuestion q) {
        AnswerToQuestion answer = answerQuestion(q.getLanguageToQuestion().get("en"), graphs);
        log.debug("Best result: " + Strings.join(answer.getAnswer(), "; "));
    }

    private AnswerToQuestion answerQuestion(String question, HashSet<String> graphs) {
        question = TextUtilities.trim(question);
        Set<String> bestAnswer;
        List<String> dBpediaProperties = DBpediaPropertiesProvider.getDBpediaProperties();
        Set<RDFNode> ontologyNodes = NTripleParser.getNodes();
        QueryMappingFactory mappingFactory = new QueryMappingFactory(question, "", Lists.newArrayList(ontologyNodes), dBpediaProperties);
        List<Map<Integer, List<String>>> results = new ArrayList<>();
        String graphPattern = semanticAnalysisHelper.classifyInstance(question, graphs);
        List<String> queries = mappingFactory.generateQueries(mappings, graphPattern, false);

        //If the template from the predicted graph won't find suitable templates, try all other templates
        if (queries.isEmpty()) {
            log.debug("There is no suitable query template for this graph, using all templates now...");
            queries = mappingFactory.generateQueries(mappings, false);
        }

        results.addAll(executeQueries(queries));

        final int expectedAnswerType = SemanticAnalysisHelper.detectQuestionAnswerType(question);
        bestAnswer = semanticAnalysisHelper.getBestAnswer(results, expectedAnswerType, false);

        //If there still is no suitable answer, use all query templates to find one
        if (bestAnswer.isEmpty()) {
            log.debug("There is no suitable answer, using all query templates instead...");
            queries = mappingFactory.generateQueries(mappings, false);
            results.addAll(executeQueries(queries));
            bestAnswer = semanticAnalysisHelper.getBestAnswer(results, expectedAnswerType, false);
        }

        //If there still is no suitable answer, use synonyms to find one
        if (bestAnswer.isEmpty()) {
            log.debug("There is no suitable answer, using synonyms to find one...");
            queries = mappingFactory.generateQueries(mappings, true);
            results.addAll(executeQueries(queries));
            bestAnswer = semanticAnalysisHelper.getBestAnswer(results, expectedAnswerType, true);
        }
        return new AnswerToQuestion(bestAnswer, mappingFactory.getRdfEntities());
    }


    private List<Map<Integer, List<String>>> executeQueries(List<String> queries) {
        List<Map<Integer, List<String>>> results = new ArrayList<>();
        queries.parallelStream().forEach(s -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Query: ").append(s);
            List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(s);
            sparqlResultSets.forEach(sparqlResultSet -> {
                List<String> result = sparqlResultSet.getResultSet();
                if (!result.isEmpty()) {
                    Map<Integer, List<String>> classifiedResult = new HashMap<>();
                    classifiedResult.put(sparqlResultSet.getType(), result);
                    results.add(classifiedResult);
                }
                sb.append("\nResult: ").append(String.join("; ", result));
                log.debug(sb.toString());
            });
        });
        return results;
    }

    private List<String> getSimpleModifiers(String queryString) {
        Pattern KEYWORD_MATCHER = Pattern.compile("\\w{2}+(?:\\s*\\w+)*");
        try {
            String trimmedQuery = semanticAnalysisHelper.cleanQuery(queryString);

            Matcher keywordMatcherCurrent = KEYWORD_MATCHER.matcher(trimmedQuery);
            List<String> modifiers = new ArrayList<>();
            while (keywordMatcherCurrent.find()) {
                String modifier = keywordMatcherCurrent.group();
                if (modifier.equalsIgnoreCase("en OPTIONAL")) {
                    modifiers.add("OPTIONAL");
                } else if (!modifier.equalsIgnoreCase("_type")
                        && !modifier.equalsIgnoreCase("en")
                        && !modifier.equalsIgnoreCase("es")) {
                    modifiers.add(modifier);
                }
            }
            return modifiers;
        } catch (QueryParseException e) {
            log.warn("Unable to parse query: " + queryString, e);
        }
        return emptyList();
    }

    private void addTrainDataset(Dataset dataset) {
        this.trainDatasets.add(dataset);
    }
}
