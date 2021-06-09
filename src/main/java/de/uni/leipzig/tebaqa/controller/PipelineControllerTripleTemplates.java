package de.uni.leipzig.tebaqa.controller;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.helper.*;
import de.uni.leipzig.tebaqa.model.*;
import edu.cmu.lti.jawjaw.pobj.POS;
//import moa.recommender.rc.utils.Hash;

import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.load.json.EJQuestionFactory;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.aksw.qa.commons.load.json.QaldJson;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.uni.leipzig.tebaqa.controller.PipelineController.getSimpleModifiers;
import static de.uni.leipzig.tebaqa.helper.TextUtilities.NON_WORD_CHARACTERS_REGEX;
import static de.uni.leipzig.tebaqa.spring.AnnotateQualD8.loadQuald9;
import static java.util.Collections.emptyList;

public class PipelineControllerTripleTemplates {
    private static Logger log = Logger.getLogger(PipelineController.class.getName());

    private static SemanticAnalysisHelper semanticAnalysisHelper;
    private List<Dataset> trainDatasets = new ArrayList<>();
    private Map<String, QueryTemplateMapping> mappings;
    private Boolean evaluateWekaAlgorithms = false;
    private Boolean recalculateWekaMaodel = false;
    Analyzer analyzer;
    HashMap<String,Classifier> classifiers;
    Attribute classAttribute;
    public PipelineControllerTripleTemplates(List<Dataset> trainDatasets) {
        log.info("Configuring controller");
        semanticAnalysisHelper = new SemanticAnalysisHelper();
        //semanticAnalysisHelper = new SemanticAnalysisHelperGerman();
        trainDatasets.forEach(this::addTrainDataset);
        log.info("Starting controller...");
        run();
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
        classAttribute = new Attribute("class", Arrays.asList(new String[]{"true","false"}));
        analyzer=ArffGeneratorTriples.getAnayzer(classAttribute);
        if(recalculateWekaMaodel) {
        List<Question> trainQuestions = new ArrayList<>();
        for (Dataset d : trainDatasets) {
            //Remove all trainQuestions without SPARQL query
            List<IQuestion> load = LoaderController.load(d);
            List<IQuestion> result = load.parallelStream()
                    .filter(question -> question.getSparqlQuery() != null)
                    .collect(Collectors.toList());
            trainQuestions.addAll(QuestionFactory.createInstances(result));
        }
        trainQuestions.addAll(QuestionFactory.createInstances(loadQuald9()));

        Map<String, String> trainQuestionsWithQuery = new HashMap<>();
        for (Question q : trainQuestions) {
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
        HashMap<String,Instances> customTrainSet;

        log.info("Building query clusters...");
        HashMap<String, Set<String>>[]commonPredicates=new HashMap[2];
        customTrainSet = transform(trainQuestionsWithQuery,commonPredicates,semanticAnalysisHelper);



        log.info("Extract query templates...");
        //mappings = semanticAnalysisHelper.extractTemplates(customTrainQuestions,commonPredicates);
        mappings=null;
        log.info("Mappings were created...");


            log.info("Creating weka model...");
            //Map<String, String> testQuestionsWithQuery = new HashMap<>();
            //only use unique trainQuestions in case multiple datasets are used
            /*for (HAWKQuestion q : trainQuestions) {
                String questionText = q.getLanguageToQuestion().get("en");
                if (!semanticAnalysisHelper.containsQuestionText(testQuestionsWithQuery, questionText)) {
                    testQuestionsWithQuery.put(q.getSparqlQuery(), questionText);
                }
            }*/
            //HashMap<String,Instances>customTestQuestions = transform(testQuestionsWithQuery, commonPredicates, semanticAnalysisHelper);
            log.info("Instantiating ArffGenerator...");
            for (String graph : customTrainSet.keySet()){
                //List<CustomQuestion> testSet = new ArrayList<>();
                //List<CustomQuestion> trainSet = new ArrayList<>();
                //if(customTestQuestions.containsKey(graph)) {
                    Instances trainSet = customTrainSet.get(graph);
                    //List<CustomQuestion> testSet = customTestQuestions.get(graph);
                    //customTrainQuestions.forEach(c->trainSet=addAll(c.getQuestions()));
                    new ArffGeneratorTriples(trainSet,graph,  evaluateWekaAlgorithms);
                    log.info("Instantiating ArffGenerator done!");

            }
        }
        HashSet<String> graphs = new HashSet<>();
        //customTrainQuestions.parallelStream().forEach(customQuestion -> graphs.add(customQuestion.getGraph()));
        classifiers=new HashMap();
        try {
            classifiers.put ("r_r_lit",(Classifier) SerializationHelper.read(new FileInputStream(new ClassPathResource("question_classification_r_r_lit.model").getFile())));
            classifiers.put ("r_r_r",(Classifier) SerializationHelper.read(new FileInputStream(new ClassPathResource("question_classification_r_r_r.model").getFile())));
            classifiers.put ("r_r_v",(Classifier) SerializationHelper.read(new FileInputStream(new ClassPathResource("question_classification_r_r_v.model").getFile())));
            classifiers.put ("v_r_lit",(Classifier) SerializationHelper.read(new FileInputStream(new ClassPathResource("question_classification_v_r_lit.model").getFile())));
            classifiers.put ("v_r_r",(Classifier) SerializationHelper.read(new FileInputStream(new ClassPathResource("question_classification_v_r_r.model").getFile())));
            classifiers.put ("v_r_v",(Classifier) SerializationHelper.read(new FileInputStream(new ClassPathResource("question_classification_v_r_v.model").getFile())));

        } catch (Exception e) {
            e.printStackTrace();
        }
        //ClassifierProvider.init(graphs);
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

    private HashMap<String,Instances> transform(Map<String, String> trainQuestionsWithQuery,HashMap<String,Set<String>>[]commonPredicates,SemanticAnalysisHelper h) {

        HashMap<String,Set<String>>triplePatternToQuery=new HashMap<>();
        Query query = new Query();
        for (String s : trainQuestionsWithQuery.keySet()) {
            try {
                ParameterizedSparqlString pss = new ParameterizedSparqlString();
                pss.append(s);
                query = pss.asQuery();
            } catch (QueryParseException e) {
                log.warn(e.toString(), e);
            }
            ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
                public void visit(ElementPathBlock el) {
                    PathBlock path = el.getPattern();
                    HashMap<Node, Node> dict = new HashMap<>();
                    int i = 1;
                    for (TriplePath t : path.getList()) {
                        String sub;
                        String pred;
                        String obj;
                        if(t.getSubject().isVariable())sub="v";
                        else sub="r";
                        if(t.getPredicate().isVariable())pred="v";
                        else pred="r";
                        if(t.getObject().isVariable())obj="v";
                        else if(t.getObject().isURI())obj="r";
                        else obj="lit";
                        String tripleTemplate=sub+"_"+pred+"_"+obj;
                        if(triplePatternToQuery.containsKey(tripleTemplate))
                            triplePatternToQuery.get(tripleTemplate).add(s);
                        else {
                            Set<String>ql=new HashSet<>();
                            ql.add(s);
                            triplePatternToQuery.put(tripleTemplate,ql);

                        }

                    }
                }
            });
        }
        HashMap<String,Instances> customTrainQuestions = new HashMap<>();
        triplePatternToQuery.forEach((key,val) -> customTrainQuestions.put(key,new Instances("training_classifier: -C 4", analyzer.fvWekaAttributes, trainQuestionsWithQuery.size())));
        trainQuestionsWithQuery.forEach((qs,ns)->{
            Instance instance=analyzer.analyze(ns);
            triplePatternToQuery.forEach((key,val) -> {
                        String contains="false";
                        if(val.contains(qs))contains="true";
                        instance.setValue(classAttribute, contains);
                        customTrainQuestions.get(key).add(instance);
            });
        });
        return customTrainQuestions;
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

    public AnswerToQuestion answerQuestion(String question) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        Instances dataset;
        attributes.add(classAttribute);
        analyzer = new Analyzer(attributes);

        ArrayList<Attribute> filteredAttributes = analyzer.fvWekaAttributes.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        dataset = new Instances("testdata", filteredAttributes, 1);
        dataset.setClassIndex(dataset.numAttributes() - 1);
        Instance instance = analyzer.analyze(question);
        instance.setDataset(dataset);
        instance.setMissing(classAttribute);

        String predictedGraph = "";
        List<TripleTemplate>neededTemplates=new ArrayList<>();
        try {
            for(String triple:classifiers.keySet()) {
                double predictedClass = classifiers.get(triple).classifyInstance(instance);
                predictedGraph = instance.classAttribute().value((int) predictedClass);
                System.out.println(triple+" : "+predictedGraph);
                if(predictedGraph.equals("true"))neededTemplates.add(new TripleTemplate(triple));
            }
        } catch (Exception e) {
            log.error(String.format("Unable to classify question: '%s'!", question), e);
        }

        //semanticAnalysisHelper=new SemanticAnalysisHelperGerman();
        FillTemplatePatternsWithResources fillTemplatePatternsWithResources=new FillTemplatePatternsWithResources(neededTemplates, semanticAnalysisHelper);

        question = TextUtilities.trim(question);
        fillTemplatePatternsWithResources.extractEntities(question);
        List<String> dBpediaProperties = null; //DBpediaPropertiesProvider.getDBpediaProperties();
        //Set<RDFNode> ontologyNodes = NTripleParser.getNodes();
        //QueryMappingFactory mappingFactory = new QueryMappingFactory(question, "", Lists.newArrayList(ontologyNodes), dBpediaProperties);
        //QueryMappingFactoryLabels mappingFactory = new QueryMappingFactoryLabels(question, "",semanticAnalysisHelper);

        Set<ResultsetBinding> results = new HashSet<>();
//        StopWatch watch = new StopWatch("QA");
//        int annotationAndQueryGenerationTotal = 0;
//        int queryExecutionTotal = 0;
//        int classificationTotal = 0;
//        watch.start("Classify Question");
        //String graphPattern = semanticAnalysisHelper.classifyInstance(question);
//        watch.stop();
//        classificationTotal += watch.getLastTaskTimeMillis();
//        watch.start("Generate Queries 1 (only matching graph)");
        return null;
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

}
