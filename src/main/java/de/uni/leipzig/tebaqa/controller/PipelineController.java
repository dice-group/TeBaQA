package de.uni.leipzig.tebaqa.controller;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.helper.*;
import de.uni.leipzig.tebaqa.model.*;
import edu.cmu.lti.jawjaw.pobj.POS;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.load.json.EJQuestionFactory;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.aksw.qa.commons.load.json.QaldJson;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.uni.leipzig.tebaqa.helper.CommonTupels.getCommonTuples;
import static de.uni.leipzig.tebaqa.helper.TextUtilities.NON_WORD_CHARACTERS_REGEX;
import static de.uni.leipzig.tebaqa.spring.AnnotateQualD8.loadLimbo;
import static de.uni.leipzig.tebaqa.spring.AnnotateQualD8.loadQuald9;
import static java.util.Collections.emptyList;


public class PipelineController {

    private static Logger log = Logger.getLogger(PipelineController.class.getName());

    private static SemanticAnalysisHelper semanticAnalysisHelper;
    private List<Dataset> trainDatasets = new ArrayList<>();
    private List<Dataset> testDatasets = new ArrayList<>();
    private Map<String, QueryTemplateMapping> mappings;
    private Boolean evaluateWekaAlgorithms = false;
    private Boolean recalculateWekaMaodel = false;


    public PipelineController(List<Dataset> trainDatasets) {
        log.info("Configuring controller");
        //semanticAnalysisHelper = new SemanticAnalysisHelper();
        semanticAnalysisHelper = new SemanticAnalysisHelperGerman();
        trainDatasets.forEach(this::addTrainDataset);
        log.info("Starting controller...");
        //run();
        runLimbo();
    }
    public PipelineController(List<Dataset> trainDatasets,List<Dataset> testDatasets) {
        log.info("Configuring controller");
        //semanticAnalysisHelper = new SemanticAnalysisHelper();
        semanticAnalysisHelper = new SemanticAnalysisHelperGerman();
        trainDatasets.forEach(this::addTrainDataset);
        testDatasets.forEach(this::addTestDataset);
        recalculateWekaMaodel=true;
        evaluateWekaAlgorithms=true;
        log.info("Starting controller...");
        //run();
        runLimbo();
    }
    public static List<IQuestion> readJson(File data) {
        List<IQuestion> out = null;
        try {
            QaldJson json = (QaldJson) ExtendedQALDJSONLoader.readJson(data);
            out = EJQuestionFactory.getQuestionsFromQaldJson(json);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return out;
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
        log.info("Building query clusters...");
        HashMap<String,Set<String>>[]commonPredicates=new HashMap[2];
        customTrainQuestions = transform(trainQuestionsWithQuery,commonPredicates);
        QueryBuilder queryBuilder = new QueryBuilder(customTrainQuestions, semanticAnalysisHelper);
        customTrainQuestions = queryBuilder.getQuestions();

        log.info("Extract query templates...");
        mappings = semanticAnalysisHelper.extractTemplates(customTrainQuestions,commonPredicates);

        log.info("Mappings were created...");
        List<String> graphs = new ArrayList<>();
        customTrainQuestions.forEach(customQuestion -> graphs.add(customQuestion.getGraph()));
        if(recalculateWekaMaodel) {
            log.info("Creating weka model...");
            Map<String, String> testQuestionsWithQuery = new HashMap<>();
            //only use unique trainQuestions in case multiple datasets are used
            /*for (HAWKQuestion q : trainQuestions) {
                String questionText = q.getLanguageToQuestion().get("de");
                if (!semanticAnalysisHelper.containsQuestionText(testQuestionsWithQuery, questionText)) {
                    testQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
                }
            }*/

            List<CustomQuestion>testSet=new ArrayList<>();
            List<CustomQuestion>trainSet=new ArrayList<>();
            customTrainQuestions.forEach(c->trainSet.addAll(c.getQuestions()));
            customTrainQuestions.forEach(c->testSet.addAll(c.getQuestions()));
            log.info("Instantiating ArffGenerator...");
            new ArffGenerator(graphs,trainSet, testSet, evaluateWekaAlgorithms);
            log.info("Instantiating ArffGenerator done!");

            log.info("Instantiating ArffGenerator done!");
        }

        ClassifierProvider.init(graphs);
//        testQuestions.parallelStream().forEach(q -> answerQuestion(graphs, q));
    }
    private void runLimbo() {
        List<HAWKQuestion> trainQuestions = new ArrayList<>();

        trainQuestions.addAll(HAWKQuestionFactory.createInstances(loadLimbo()));

        Map<String, String> trainQuestionsWithQuery = new HashMap<>();
        for (HAWKQuestion q : trainQuestions) {
            //only use unique trainQuestions in case multiple datasets are used
            String questionText = q.getLanguageToQuestion().get("de");
            if (!semanticAnalysisHelper.containsQuestionText(trainQuestionsWithQuery, questionText)) {
                trainQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
            }
        }

        //log.info("Generating ontology mapping...");
        //createOntologyMapping(trainQuestionsWithQuery);
        //log.info("Ontology Mapping: " + OntologyMappingProvider.getOntologyMapping());

        //List<CustomQuestion> customTrainQuestions;
        List<Cluster> customTrainQuestions;
        log.info("Building query clusters...");
        HashMap<String,Set<String>>[]commonPredicates=new HashMap[2];
        customTrainQuestions = transform(trainQuestionsWithQuery,commonPredicates);
        QueryBuilder queryBuilder = new QueryBuilder(customTrainQuestions, semanticAnalysisHelper);
        customTrainQuestions = queryBuilder.getQuestions();

        log.info("Extract query templates...");
        mappings = semanticAnalysisHelper.extractTemplates(customTrainQuestions,commonPredicates);

        log.info("Mappings were created...");
        /*List<String> graphs = new ArrayList<>();
        customTrainQuestions.forEach(customQuestion -> graphs.add(customQuestion.getGraph()));
        if(recalculateWekaMaodel) {
            log.info("Creating weka model...");
            Map<String, String> testQuestionsWithQuery = new HashMap<>();
            //only use unique trainQuestions in case multiple datasets are used
            for (HAWKQuestion q : trainQuestions) {
                String questionText = q.getLanguageToQuestion().get("de");
                if (!semanticAnalysisHelper.containsQuestionText(testQuestionsWithQuery, questionText)) {
                    testQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
                }
            }

            List<CustomQuestion>testSet=new ArrayList<>();
            List<CustomQuestion>trainSet=new ArrayList<>();
            customTrainQuestions.forEach(c->trainSet.addAll(c.getQuestions()));
            customTrainQuestions.forEach(c->testSet.addAll(c.getQuestions()));
            log.info("Instantiating ArffGenerator...");
            new ArffGenerator(graphs,trainSet, testSet, evaluateWekaAlgorithms);
            log.info("Instantiating ArffGenerator done!");

            log.info("Instantiating ArffGenerator done!");
        }

        ClassifierProvider.init(graphs);*/
//        testQuestions.parallelStream().forEach(q -> answerQuestion(graphs, q));
    }
    /*private void run() {

        List<HAWKQuestion> trainQuestions = new ArrayList<>();
        List<IQuestion> load = readJson(new File("limboqa.json"));

        List<IQuestion> result = load.parallelStream()
                .filter(question -> question.getSparqlQuery() != null)
                .collect(Collectors.toList());
        trainQuestions.addAll(HAWKQuestionFactory.createInstances(result));
        Map<String, String> trainQuestionsWithQuery = new HashMap<>();
        for (HAWKQuestion q : trainQuestions) {
            //only use unique trainQuestions in case multiple datasets are used
            String questionText = q.getLanguageToQuestion().get("de");
            if (!semanticAnalysisHelper.containsQuestionText(trainQuestionsWithQuery, questionText)) {
                trainQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
            }
        }

        //log.info("Generating ontology mapping...");
        //createOntologyMapping(trainQuestionsWithQuery);
        //log.info("Ontology Mapping: " + OntologyMappingProvider.getOntologyMapping());

        log.info("Getting DBpedia properties from SPARQL endpoint...");
        List<String> dBpediaProperties = null;//DBpediaPropertiesProvider.getDBpediaProperties();

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
        //only use unique trainQuestions in case multiple datasets are used
        for (HAWKQuestion q : trainQuestions) {
            String questionText = q.getLanguageToQuestion().get("de");
            if (!semanticAnalysisHelper.containsQuestionText(testQuestionsWithQuery, questionText)) {
                testQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
            }
        }
        log.info("Instantiating ArffGenerator...");
        new ArffGenerator(customTrainQuestions, transform(testQuestionsWithQuery), evaluateWekaAlgorithms);
        log.info("Instantiating ArffGenerator done!");

        HashSet<String> graphs = new HashSet<>();
        customTrainQuestions.parallelStream().forEach(customQuestion -> graphs.add(customQuestion.getGraph()));
        ClassifierProvider.init(graphs);

//        testQuestions.parallelStream().forEach(q -> answerQuestion(graphs, q));
    }*/
    private List<Cluster> transform(Map<String, String> trainQuestionsWithQuery,HashMap<String,Set<String>>[]commonPredicates) {

        List<CustomQuestion> customTrainQuestions = new ArrayList<>();
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(trainQuestionsWithQuery,commonPredicates);
        List<Cluster> clusters = queryIsomorphism.getClusters();

        return clusters;
    }

    /*private List<CustomQuestion> transform(Map<String, String> trainQuestionsWithQuery,HashMap<String,Set<String>>[]commonPredicates) {
        List<CustomQuestion> customTrainQuestions = new ArrayList<>();
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(trainQuestionsWithQuery,commonPredicates);
        List<Cluster> clusters = queryIsomorphism.getClusters();
        for (Cluster cluster : clusters) {
            String graph = cluster.getGraph();
            List<Question> questionList = cluster.getQuestions();
            for (Question question : questionList) {
                String questionText = question.getLanguageToQuestion().get("en");
                String sparqlQuery = question.getSparqlQuery();
                List<String> simpleModifiers = getSimpleModifiers(sparqlQuery);
                List<SPARQLResultSet> sparqlResultSets = SPARQLUtilities.executeSPARQLQuery(sparqlQuery);
                List<String> resultSet = new ArrayList<>();
                sparqlResultSets.forEach(sparqlResultSet -> resultSet.addAll(sparqlResultSet.getResultSet()));
                customTrainQuestions.add(new CustomQuestion(sparqlQuery, questionText, simpleModifiers, graph));
//                semanticAnalysisHelper.annotate(questionText);
            }
        }
        return customTrainQuestions;
    }*/

    private void createOntologyMapping(Map<String, String> questionsWithQuery) {
        Map<String, Set<String>> lemmaOntologyMapping = new HashMap<>();
        questionsWithQuery.keySet().parallelStream().forEach((sparqlQuery) -> {
            WordNetWrapper wordNetWrapper = new WordNetWrapper();
            String questionText = questionsWithQuery.get(sparqlQuery);
            ArrayList<String> words = Lists.newArrayList(questionText.split(NON_WORD_CHARACTERS_REGEX));
            Matcher matcher = Utilities.BETWEEN_LACE_BRACES.matcher(SPARQLUtilities.resolveNamespaces(sparqlQuery));
            while (matcher.find()) {
                String entity = matcher.group().replace("<", "").replace(">", "");
                if (!entity.startsWith("http://dbpedia.org/resource/")) {
                    for (String word : words) {
                        Map<String, String> lemmas = semanticAnalysisHelper.getLemmas(word);
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
                        String wordPosString = semanticAnalysisHelper.getPOS(lemma).getOrDefault(lemma, "");
                        POS currentWordPOS = PosTransformation.transform(wordPosString);
                        String posStringOfEntity = semanticAnalysisHelper.getPOS(entityName).getOrDefault(entityName, "");
                        POS entityPOS = PosTransformation.transform(posStringOfEntity);
                        Double similarity;
                        if (entityPOS == null || currentWordPOS == null) {
                            continue;
                        }
                        if (entityName.length() > 1 && SemanticAnalysisHelper.countUpperCase(entityName.substring(1, entityName.length() - 1)) > 0) {
                            similarity = wordNetWrapper.semanticSimilarityBetweenWordgroupAndWord(entityName, lemma,semanticAnalysisHelper);
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
    public AnswerToQuestion answerLimboQuestion(String question) {
        //semanticAnalysisHelper=new SemanticAnalysisHelperGerman();
        question = TextUtilities.trim(question);
        //QueryMappingFactory mappingFactory = new QueryMappingFactory(question, "", Lists.newArrayList(ontologyNodes), dBpediaProperties);
        QueryMappingFactoryLabels mappingFactory = new QueryMappingFactoryLabels(question, "",semanticAnalysisHelper);

//          StopWatch watch = new StopWatch("QA");
//        int annotationAndQueryGenerationTotal = 0;
//        int queryExecutionTotal = 0;
//        int classificationTotal = 0;
//        watch.start("Classify Question");
        FillTemplatePatternsWithResources tripleGenerator=new FillTemplatePatternsWithResources(semanticAnalysisHelper);
        tripleGenerator.extractEntities(question);
        List<RatedQuery> ratedQueries = mappingFactory.generateQueries(mappings, null, tripleGenerator);
        List<String> queries=new ArrayList<>();
        queries = ratedQueries.stream().map(RatedQuery::getQuery).collect(Collectors.toList());
        //If the template from the predicted graph won't find suitable templates, try all other templates
        List<ResultsetBinding>queryResults=new ArrayList<>();
        for(int i=0;i<ratedQueries.size();i++){
            RatedQuery ratedQuery = ratedQueries.get(i);
            ResultsetBinding results = SPARQLUtilities.executeQuery(ratedQuery.getQuery());
            if(!results.getResult().isEmpty()) {
                results.setRating(ratedQuery.getRating());
                queryResults.add(results);
            }
        }
        //if (queries.isEmpty()) {
//            watch.stop();
//            annotationAndQueryGenerationTotal += watch.getLastTaskTimeMillis();
//            watch.start("Generate Queries 2 (all templates)");
        //  log.info("There is no suitable query template for this graph, using all templates now...");
        //queries = mappingFactory.generateQueries(mappings, false);
        //queries = mappingFactory.generateQueriesWithResourceLinker(mappings,null,links);
        //}

//        watch.stop();
//        annotationAndQueryGenerationTotal += watch.getLastTaskTimeMillis();
//        watch.start("Execute Queries 1");
        //results.addAll(executeQueries(queries));
        final int expectedAnswerType = semanticAnalysisHelper.detectQuestionAnswerType(question);
        //ResultsetBinding rsBinding = semanticAnalysisHelper.getBestAnswer(results,links, mappingFactory.getEntitiyToQuestionMapping(), expectedAnswerType, false);

        ResultsetBinding rsBinding=semanticAnalysisHelper.getBestAnswer(queryResults,expectedAnswerType, false);

        rsBinding.retrieveRedirects();
        return new AnswerToQuestion(rsBinding, mappingFactory.getEntitiyToQuestionMapping());
    }
    public AnswerToQuestion answerQuestion(String question) {
        //semanticAnalysisHelper=new SemanticAnalysisHelperGerman();
        question = TextUtilities.trim(question);
        List<String> dBpediaProperties = null; //DBpediaPropertiesProvider.getDBpediaProperties();
        Set<RDFNode> ontologyNodes = NTripleParser.getNodes();
        //QueryMappingFactory mappingFactory = new QueryMappingFactory(question, "", Lists.newArrayList(ontologyNodes), dBpediaProperties);
        QueryMappingFactoryLabels mappingFactory = new QueryMappingFactoryLabels(question, "",semanticAnalysisHelper);

//          StopWatch watch = new StopWatch("QA");
//        int annotationAndQueryGenerationTotal = 0;
//        int queryExecutionTotal = 0;
//        int classificationTotal = 0;
//        watch.start("Classify Question");
        FillTemplatePatternsWithResources tripleGenerator=new FillTemplatePatternsWithResources(semanticAnalysisHelper);
        tripleGenerator.extractEntities(question);
        String graphPattern = semanticAnalysisHelper.classifyInstance(question);
        System.out.println(question+":"+graphPattern);
// watch.stop();
//        classificationTotal += watch.getLastTaskTimeMillis();
//        watch.start("Generate Queries 1 (only matching graph)");
        //ResourceLinker links=new ResourceLinker(semanticAnalysisHelper);
        //links.extractEntities(question);
        //Set<String> queries = mappingFactory.generateQueriesWithResourceLinker(mappings, graphPattern, false);
        //HashMap<String,String> queries = mappingFactory.generateQueriesWithResourceLinker(mappings, graphPattern, links);
        List<RatedQuery> ratedQueries = mappingFactory.generateQueries(mappings, graphPattern, tripleGenerator);
        List<String> queries=new ArrayList<>();
        queries = ratedQueries.stream().map(RatedQuery::getQuery).collect(Collectors.toList());
        //If the template from the predicted graph won't find suitable templates, try all other templates
        List<ResultsetBinding>queryResults=new ArrayList<>();
        for(int i=0;i<10&&i<queries.size();i++){
            queryResults.add(SPARQLUtilities.executeQuery(queries.get(i)));
        }
        //if (queries.isEmpty()) {
//            watch.stop();
//            annotationAndQueryGenerationTotal += watch.getLastTaskTimeMillis();
//            watch.start("Generate Queries 2 (all templates)");
          //  log.info("There is no suitable query template for this graph, using all templates now...");
            //queries = mappingFactory.generateQueries(mappings, false);
            //queries = mappingFactory.generateQueriesWithResourceLinker(mappings,null,links);
        //}

//        watch.stop();
//        annotationAndQueryGenerationTotal += watch.getLastTaskTimeMillis();
//        watch.start("Execute Queries 1");
        //results.addAll(executeQueries(queries));
        final int expectedAnswerType = semanticAnalysisHelper.detectQuestionAnswerType(question);
        //ResultsetBinding rsBinding = semanticAnalysisHelper.getBestAnswer(results,links, mappingFactory.getEntitiyToQuestionMapping(), expectedAnswerType, false);
        ResultsetBinding rsBinding=semanticAnalysisHelper.getBestAnswer(queryResults,expectedAnswerType, false);

        //If there still is no suitable answer, use all query templates to find one
        if (rsBinding.getResult().isEmpty()) {
            //log.info("There is no suitable answer, using all query templates instead...");
//            watch.stop();
//            queryExecutionTotal += watch.getLastTaskTimeMillis();
//            watch.start("Generate Queries 3 (all templates)");
            ratedQueries = mappingFactory.generateQueries(mappings, null, tripleGenerator);
            queries = ratedQueries.stream().map(RatedQuery::getQuery).collect(Collectors.toList());
            queryResults=new ArrayList<>();
            for(int i=0;i<10&&i<queries.size();i++){
                queryResults.add(SPARQLUtilities.executeQuery(queries.get(i)));
            }
            //rsBinding=semanticAnalysisHelper.getBestAnswer(queryResults,expectedAnswerType, false);
            rsBinding=semanticAnalysisHelper.getBestAnswer(queryResults,expectedAnswerType, true);
        }
            //System.out.println(graphPattern);
//            watch.stop();
//            annotationAndQueryGenerationTotal += watch.getLastTaskTimeMillis();
//            watch.start("Execute Queries 2 (all templates)");
            //results.addAll(executeQueries(queries));
            //bestAnswer = semanticAnalysisHelper.getBestAnswer(results, expectedAnswerType, false);
            //rsBinding = semanticAnalysisHelper.getBestAnswer(results,links, mappingFactory.getEntitiyToQuestionMapping(), expectedAnswerType, true);
          //  rsBinding=semanticAnalysisHelper.getBestAnswer(results,expectedAnswerType, true);
        //}

        //If there still is no suitable answer, use synonyms to find one
        /*if (rsBinding.getResult().isEmpty()) {
            log.info("There is no suitable answer, using synonyms to find one...");
//            watch.stop();
//            queryExecutionTotal += watch.getLastTaskTimeMillis();
//            watch.start("Generate Queries 4 (all templates & synonyms)");
            queries = mappingFactory.generateQueries(mappings, true);
//            watch.stop();
//            annotationAndQueryGenerationTotal += watch.getLastTaskTimeMillis();
//            watch.start("Execute Queries 3 ((all templates & synonyms)");
            results.addAll(executeQueries(queries));
            rsBinding = semanticAnalysisHelper.getBestAnswer(results, mappingFactory.getEntitiyToQuestionMapping(), expectedAnswerType, true);
        }*/
//        watch.stop();
//        queryExecutionTotal += watch.getLastTaskTimeMillis();
//        log.info("Classification total: (, " + classificationTotal + ")");
//        log.info("Annotation & Query generation total: (," + annotationAndQueryGenerationTotal + ")");
//        log.info("Query execution total: (," + queryExecutionTotal + ")");
        rsBinding.retrieveRedirects();
        return new AnswerToQuestion(rsBinding, mappingFactory.getEntitiyToQuestionMapping());
    }

    private Set<ResultsetBinding> executeQueries(HashMap<String,String> queries) {
        Set<ResultsetBinding> bindings = new HashSet<>();

        for (String s : queries.keySet()) {
            bindings.addAll(SPARQLUtilities.retrieveBinings(s,queries.get(s)));
        }
        return bindings.stream()
                .filter(binding -> binding.getResult().size() > 0)
                .collect(Collectors.toSet());
    }

    public static List<String> getSimpleModifiers(String queryString) {
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
    private void addTestDataset(Dataset dataset) {
        this.testDatasets.add(dataset);
    }

    public Map<String, QueryTemplateMapping> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, QueryTemplateMapping> mappings) {
        this.mappings = mappings;
    }
}
