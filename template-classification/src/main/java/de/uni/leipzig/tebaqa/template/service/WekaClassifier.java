package de.uni.leipzig.tebaqa.template.service;

//import de.uni.leipzig.tebaqa.tebaqacommons.model.Cluster;
//import de.uni.leipzig.tebaqa.tebaqacommons.model.CustomQuestion;
//import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryIsomorphism;

import de.uni.leipzig.tebaqa.template.model.Cluster;
import de.uni.leipzig.tebaqa.template.model.CustomQuestion;
import de.uni.leipzig.tebaqa.template.model.QueryIsomorphism;
import de.uni.leipzig.tebaqa.template.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.template.nlp.ClassifierProvider;
import de.uni.leipzig.tebaqa.template.nlp.ISemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.template.nlp.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.template.util.Constants;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import weka.classifiers.Classifier;
import weka.core.Instance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class WekaClassifier {

    private static final Logger LOGGER = Logger.getLogger(WekaClassifier.class.getName());
    private static WekaClassifier instance;
    private static Dataset trainDataset;
    private static Dataset testDataset;
    private static ISemanticAnalysisHelper semanticAnalysisHelper;
    private static Map<String, QueryTemplateMapping> graphToQueryTemplateMappings;
    private static List<String> graphs;
    private static Classifier classifier;
    private Boolean evaluateWekaAlgorithms = false;
    private Boolean recalculateWekaModel = false;

    private WekaClassifier() {
    }

//    private WekaClassifier(Dataset trainDataset) {
//        LOGGER.info("Configuring controller");
//        this.semanticAnalysisHelper = new SemanticAnalysisHelperEnglish();
//        this.trainDataset = trainDataset;
//        LOGGER.info("Starting controller...");
//        trainClassifier();
//    }
//
//    private WekaClassifier(Dataset trainDataset, Dataset testDataset) {
//        LOGGER.info("Configuring controller");
//        this.trainDataset = trainDataset;
//        this.testDataset = testDataset;
//        recalculateWekaModel = true;
//        evaluateWekaAlgorithms = true;
//        LOGGER.info("Starting controller...");
//        trainClassifier();
//    }

    public static WekaClassifier getDefaultClassifier() {
        LOGGER.info("Configuring controller");
//        semanticAnalysisHelper = new SemanticAnalysisHelperEnglish();

//        isTrainingRequired();
        // Default name
        String serializedClassifierFile = null;

        // Check if filename present in properties file
        try {
            Properties p = new Properties();
            p.load(new ClassPathResource(Constants.TEMPLATE_CLASSIFICATION_PROP_FILE).getInputStream());
            if (p.containsKey(Constants.SERIALIZED_CLASSIFIER_FILE)) {
                serializedClassifierFile = p.getProperty(Constants.SERIALIZED_CLASSIFIER_FILE);
            }
        } catch (IOException e) {
            LOGGER.error("Could not load properties from " + Constants.TEMPLATE_CLASSIFICATION_PROP_FILE);
        }

        if (serializedClassifierFile == null)
            serializedClassifierFile = Constants.DEFAULT_SERIALIZED_CLASSIFIER_FILENAME;

//        if(!Paths.get(serializedClassifierFile).toFile().exists()) {
        trainDataset = Dataset.QALD8_Train_Multilingual; // TODO Externalize
        LOGGER.info("Starting controller...");
        trainClassifier();
//        }
//        ClassifierProvider.init()
        return instance;
    }

    private static List<Cluster> clusterQueries() {
        //Remove all trainQuestions without SPARQL query
        List<IQuestion> load = LoaderController.load(trainDataset);
        List<IQuestion> result = load.parallelStream()
                .filter(question -> question.getSparqlQuery() != null)
                .collect(Collectors.toList());
        List<HAWKQuestion> trainQuestions = new ArrayList<>(HAWKQuestionFactory.createInstances(result));

        Map<String, String> trainQuestionsWithQuery = new HashMap<>();
        trainQuestions.forEach(trainQuestion -> trainQuestionsWithQuery.put(trainQuestion.getSparqlQuery(), trainQuestion.getLanguageToQuestion().get("en")));

        //log.info("Generating ontology mapping...");
        //createOntologyMapping(trainQuestionsWithQuery);
        //log.info("Ontology Mapping: " + OntologyMappingProvider.getOntologyMapping());

        //List<CustomQuestion> queryClusters;
        List<Cluster> queryClusters;
        LOGGER.info("Building query clusters...");

        // Clustering queries
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(trainQuestionsWithQuery);
        queryClusters = queryIsomorphism.getClusters();
//        QueryBuilder queryBuilder = new QueryBuilder(queryClusters, semanticAnalysisHelper);
//        queryClusters = queryBuilder.getQuestions();
        LOGGER.info("Mappings were created...");


        return queryClusters;
    }

    private static void trainClassifier() {
        // Create clusters from questions
        List<Cluster> queryClusters = clusterQueries();

        List<String> graphs = new ArrayList<>();
        queryClusters.forEach(customQuestion -> graphs.add(customQuestion.getGraph()));

        // Output the graphs
        saveGraphs(graphs);

        LOGGER.info("Extract query templates...");
        graphToQueryTemplateMappings = semanticAnalysisHelper.mapGraphToTemplates(queryClusters);

        saveMappings(graphToQueryTemplateMappings);

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
        queryClusters.forEach(c -> trainSet.addAll(c.getQuestions()));
        queryClusters.forEach(c -> testSet.addAll(c.getQuestions()));
        LOGGER.info("Instantiating ArffGenerator...");
//        new ArffGenerator(graphs, trainSet, testSet, false); //TODO changed from evaluateWekaAlgo to false
        LOGGER.info("Instantiating ArffGenerator done!");

        LOGGER.info("Instantiating ArffGenerator done!");
//        }

        ClassifierProvider.init(graphs);
//        testQuestions.parallelStream().forEach(q -> answerQuestion(graphs, q));
    }

    private static void saveMappings(Map<String, QueryTemplateMapping> graphToQueryTemplateMappings) {

    }

    private static void saveGraphs(List<String> graphs) {
        String graphsFileName = null;

        // Check if filename present in properties file
        try {
            Properties p = new Properties();
            p.load(new ClassPathResource(Constants.TEMPLATE_CLASSIFICATION_PROP_FILE).getInputStream());
            if (p.containsKey(Constants.SERIALIZED_GRAPHS_FILE)) {
                graphsFileName = p.getProperty(Constants.SERIALIZED_GRAPHS_FILE);
            }
        } catch (IOException e) {
            LOGGER.error("Could not load properties from " + Constants.TEMPLATE_CLASSIFICATION_PROP_FILE);
        }

        if (graphsFileName == null)
            graphsFileName = Constants.DEFAULT_SERIALIZED_GRAPHS_FILENAME;

        File graphsFile;
        try {
            ClassPathResource fileResource = new ClassPathResource(graphsFileName);
            if (!fileResource.exists()) {
                String fileResourcePath = fileResource.getPath();
                graphsFile = new File(fileResourcePath);
                graphsFile.createNewFile();
            } else {
                graphsFile = fileResource.getFile();
            }

            try (BufferedReader writer = new BufferedReader(new FileReader(graphsFile))) {
                String line = writer.readLine();
                while (line != null) {
                    System.out.println(line);
                    line = writer.readLine();
                }
            } catch (IOException e) {
                LOGGER.error("Cannot write graphs file to " + graphsFileName);
            }

//            try (BufferedWriter writer = new BufferedWriter(new FileWriter(graphsFile))) {
//                for (String graph : graphs) {
//                    writer.append(graph);
//                    writer.newLine();
//                }
//            } catch (IOException e) {
//                LOGGER.error("Cannot write graphs file to " + graphsFileName);
//            }
        } catch (IOException e) {
            LOGGER.error("Cannot open graphs file " + graphsFileName);
        }

    }

    private static List<Cluster> transform(Map<String, String> trainQuestionsWithQuery, HashMap<String, Set<String>>[] commonPredicates) {

        List<CustomQuestion> customTrainQuestions = new ArrayList<>();
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(trainQuestionsWithQuery);

        return queryIsomorphism.getClusters();
    }

    public static void main(String[] args) throws IOException {
        WekaClassifier.saveGraphs(new ArrayList<>());
//        WekaClassifier o = WekaClassifier.getDefaultClassifier();
//        String absolutePath = new ClassPathResource("template-classification.properties").getFile().getAbsolutePath();
//        BufferedReader bufferedReader = new BufferedReader(new FileReader(absolutePath));
////        String line = bufferedReader.readLine()
////        while(; line != null)
//        Stream<String> lines = Files.lines(Paths.get(absolutePath));
//        lines.forEach(System.out::println);
//        FileInputStream stream = new FileInputStream(new ClassPathResource("question_classificationww.model").getFile());
//        System.out.println(stream);


//        Properties p = new Properties();
//        p.load(new ClassPathResource(Constants.TEMPLATE_CLASSIFICATION_PROP_FILE).getInputStream());
//        System.getProperties().putAll(p);
//        System.getProperties().putAll(p);
//        System.out.println(System.getProperties().getProperty("classifier.model.file"));

    }

    /**
     * Classifies a question and tries to find the best matching graph pattern for it's SPARQL query.
     *
     * @param question The question which shall be classified.
     * @return The predicted graph pattern.
     */
    public String classifyInstance(String question) {
        Analyzer analyzer = ClassifierProvider.getAnalyzer();
        Instance instance = analyzer.analyze(question);
        instance.setDataset(ClassifierProvider.getDataset());
        instance.setMissing(ClassifierProvider.getClassAttribute());

        String predictedGraph = "";
        try {
            Classifier cls = ClassifierProvider.getSingletonClassifierInstance();
            double predictedClass = cls.classifyInstance(instance);
            predictedGraph = instance.classAttribute().value((int) predictedClass);
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to classify question: '%s'!", question), e);
        }
        return predictedGraph;
    }
}
