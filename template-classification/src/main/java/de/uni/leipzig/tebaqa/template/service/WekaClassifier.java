package de.uni.leipzig.tebaqa.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.StanfordPipelineProvider;
import de.uni.leipzig.tebaqa.tebaqacommons.util.JSONUtils;
import de.uni.leipzig.tebaqa.template.model.Cluster;
import de.uni.leipzig.tebaqa.template.model.CustomQuestion;
import de.uni.leipzig.tebaqa.template.model.QueryIsomorphism;
import de.uni.leipzig.tebaqa.template.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.template.nlp.ArffGenerator;
import de.uni.leipzig.tebaqa.template.nlp.ClassifierProvider;
import de.uni.leipzig.tebaqa.template.nlp.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.template.util.Constants;
import de.uni.leipzig.tebaqa.template.util.PropertyUtils;
import de.uni.leipzig.tebaqa.template.util.Utilities;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.load.json.EJQuestionFactory;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.aksw.qa.commons.load.json.QaldJson;
import org.apache.log4j.Logger;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class WekaClassifier {

    private static final Logger LOGGER = Logger.getLogger(WekaClassifier.class.getName());
    private static final Dataset DEFAULT_TRAINING_DATASET = Dataset.QALD9_Train_Multilingual;

    private static final Map<Dataset, WekaClassifier> classifierInstances = new HashMap<>(1);
    private final Dataset trainDataset;
    private SemanticAnalysisHelper semanticAnalysisHelper;
    private Map<String, QueryTemplateMapping> graphToQueryTemplateMappings;
    private List<String> graphs;
//    private Dataset testDataset;
//    private Classifier classifier;
//    private final Boolean evaluateWekaAlgorithms = false;
//    private final Boolean recalculateWekaModel = false;

    private WekaClassifier(Dataset dataset) {
        this.trainDataset = dataset;
    }

    public static WekaClassifier getDefaultClassifier() throws IOException {
        String trainingDatasetName = PropertyUtils.getProperty(Constants.DEFAULT_TRAINING_DATASET);
        Dataset trainingDataset = DEFAULT_TRAINING_DATASET;
        for(Dataset d : Dataset.values()) {
            if(d.name().equalsIgnoreCase(trainingDatasetName)) {
                LOGGER.info("Training dataset found in properties file: " + trainingDatasetName);
                trainingDataset = d;
                break;
            }
        }
        return getClassifierFor(trainingDataset);
    }

    public static WekaClassifier getClassifierFor(Dataset dataset) throws IOException {
        LOGGER.info("Preparing classifier for dataset: " + dataset.name());
        WekaClassifier classifierInstance = classifierInstances.get(dataset);
        if (classifierInstance == null) {
            classifierInstance = new WekaClassifier(dataset);
            classifierInstance.initClassifier();
            classifierInstances.put(dataset, classifierInstance);
        }
        return classifierInstance;
    }

    // Perform basic steps to ensure classifier instance is ready
    private void initClassifier() throws IOException {
        LOGGER.info("Initializing Weka classifier");
        this.semanticAnalysisHelper = new SemanticAnalysisHelper(Lang.EN);

        this.loadGraphs();
        this.loadMappings();

        if (this.graphs == null || this.graphToQueryTemplateMappings == null ||
                !this.isClassifierFilePresent()) {
            LOGGER.info("Training classifier model using " + this.trainDataset.name());
            this.trainClassifier();
            LOGGER.info("Training done");
        }
        ClassifierProvider.init(this.trainDataset.name(), this.graphs);
    }

    private boolean isClassifierFilePresent() {
        return new File(PropertyUtils.getClassifierFileAbsolutePath(trainDataset.name())).exists();
    }

    private void trainClassifier() {
        // 1. Create clusters from questions
        LOGGER.info("Clustering questions ...");
        List<Cluster> queryClusters = clusterQueries(trainDataset);
        LOGGER.info(queryClusters.size() + " clusters created.");

        // 3. Extract and save (graph<->query templates) mappings
        LOGGER.info("Extracting query templates ...");
        this.graphToQueryTemplateMappings = Utilities.mapGraphToTemplates(queryClusters);
        saveMappings(graphToQueryTemplateMappings);
        LOGGER.info("Mappings saved to file.");

        // 2. Extract and save graphs
        LOGGER.info("Extracting graphs ...");
        this.graphs = new ArrayList<>();
        graphs.addAll(graphToQueryTemplateMappings.keySet());
        saveGraphs(graphs);
        LOGGER.info("Graphs saved to file.");

        // 4. Train classifier
        LOGGER.info("Training classifier...");
        List<CustomQuestion> trainSet = new ArrayList<>();
        queryClusters.forEach(c -> trainSet.addAll(c.getQuestions()));
        LOGGER.info("Total question: " + trainSet.size());
//        List<CustomQuestion> testSet = new ArrayList<>();
//        queryClusters.forEach(c -> testSet.addAll(c.getQuestions()));
        LOGGER.info("Instantiating ArffGenerator...");
        new ArffGenerator(trainDataset.name(), this.graphs, trainSet);
        LOGGER.info("Instantiating ArffGenerator done!");

        ClassifierProvider.init(trainDataset.name(), graphs);
    }

    private static List<Cluster> clusterQueries(Dataset dataset) {
//        List<HAWKQuestion> trainQuestions = loadTrainingQuestions(dataset);
        List<HAWKQuestion> trainQuestions = loadTrainingQuestions(dataset);

        Map<String, String> trainQuestionsWithQuery = new HashMap<>();
        trainQuestions.forEach(trainQuestion -> trainQuestionsWithQuery.put(trainQuestion.getSparqlQuery(), trainQuestion.getLanguageToQuestion().get("en").replace("{", "").replace("}", "")));

        //log.info("Generating ontology mapping...");
        //createOntologyMapping(trainQuestionsWithQuery);
        //log.info("Ontology Mapping: " + OntologyMappingProvider.getOntologyMapping());

        //List<CustomQuestion> queryClusters;
        List<Cluster> queryClusters;
        LOGGER.info("Building query clusters...");

        // Clustering queries
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(trainQuestionsWithQuery);
        queryClusters = queryIsomorphism.getClusters();

        // TODO discuss, creates POS tags, which are not used later
//        QueryBuilder queryBuilder = new QueryBuilder(queryClusters, semanticAnalysisHelper);
//        queryClusters = queryBuilder.getQuestions();

        return queryClusters;
    }

    private static List<HAWKQuestion> loadLCQuadTrainingQuestions() {
        QaldJson json;
        List<IQuestion> out = null;
        try {
            json = (QaldJson) ExtendedQALDJSONLoader.readJson(new FileInputStream(new File(PropertyUtils.getProperty("files.lcquad.train"))), QaldJson.class);
            out = EJQuestionFactory.getQuestionsFromQaldJson(json);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return HAWKQuestionFactory.createInstances(out);
    }

    private static List<HAWKQuestion> loadTrainingQuestions(Dataset dataset) {
        if(dataset == Dataset.LCQUAD) {
            return loadLCQuadTrainingQuestions();
        } else if(dataset == Dataset.QALD9_Train_Multilingual) {
            return loadQALD9Training();
        } else {
            //Remove all trainQuestions without SPARQL query
            List<IQuestion> load = LoaderController.load(dataset);
            List<IQuestion> result = load.parallelStream()
                    .filter(question -> question.getSparqlQuery() != null)
                    .collect(Collectors.toList());
            return HAWKQuestionFactory.createInstances(result);
        }
    }

    public static List<HAWKQuestion> loadQALD9Training() {
        QaldJson json = null;
        List<IQuestion> out = null;
        String deriveUri = null;
        try {
            json = (QaldJson) ExtendedQALDJSONLoader.readJson(new FileInputStream(new File("qald-9-train-multilingual.json")), QaldJson.class);
            out = EJQuestionFactory.getQuestionsFromQaldJson(json);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return HAWKQuestionFactory.createInstances(out);
    }


    private void saveGraphs(List<String> graphs) {
        String graphsFilePath = PropertyUtils.getGraphsFileAbsolutePath(this.trainDataset.name());
        LOGGER.info("Saving graphs in " + graphsFilePath);

        try {
            File graphsFile = new File(graphsFilePath);

            if (!graphsFile.exists()) {
                boolean newFileCreated = graphsFile.createNewFile();
                if (newFileCreated) LOGGER.info("New file created at " + graphsFilePath);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(graphsFile))) {
                for (String graph : graphs) {
                    writer.append(graph);
                    writer.newLine();
                }
            } catch (IOException e) {
                LOGGER.error("Cannot write graphs file to " + graphsFilePath);
            }

        } catch (IOException e) {
            LOGGER.error("Cannot open graphs file " + graphsFilePath);
        }
    }

    private void loadGraphs() {
        List<String> graphsFromFile = new ArrayList<>();

        String graphsFilePath = PropertyUtils.getGraphsFileAbsolutePath(this.trainDataset.name());
        File graphsFile = new File(graphsFilePath);

        if (!graphsFile.exists()) {
            LOGGER.info("Graphs file does not exist at " + graphsFilePath);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(graphsFile))) {
            String graphPattern = reader.readLine();
            while (graphPattern != null) {
                graphsFromFile.add(graphPattern);
                graphPattern = reader.readLine();
            }

        } catch (IOException e) {
            LOGGER.error("Cannot read graphs from file " + graphsFilePath);
            LOGGER.error(e.getMessage());
            graphsFromFile = null;
        }

        this.graphs = graphsFromFile;
    }

    private void saveMappings(Map<String, QueryTemplateMapping> graphToQueryTemplateMappings) {
        String mappingsFilePath = PropertyUtils.getMappingsFileAbsolutePath(this.trainDataset.name());
        LOGGER.info("Saving mappings in " + mappingsFilePath);

        try {
            File mappingsFile = new File(mappingsFilePath);

            if (!mappingsFile.exists()) {
                boolean newFileCreated = mappingsFile.createNewFile();
                if (newFileCreated) LOGGER.info("New file created at " + mappingsFilePath);
            }

            ObjectMapper mapper = new ObjectMapper();
            String mappingsJson = mapper.writeValueAsString(graphToQueryTemplateMappings);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(mappingsFile))) {
                writer.append(mappingsJson);
                writer.newLine();
            } catch (IOException e) {
                LOGGER.error("Cannot write mappings file to " + mappingsFilePath);
            }

        } catch (IOException e) {
            LOGGER.error("Cannot open mappings file " + mappingsFilePath);
        }
    }

    private void loadMappings() {
        Map<String, QueryTemplateMapping> mappingsFromFile = new HashMap<>();
        String mappingsFilePath = PropertyUtils.getMappingsFileAbsolutePath(this.trainDataset.name());

        File mappingsFile = new File(mappingsFilePath);
        if (!mappingsFile.exists()) {
            LOGGER.warn("Mappings file does not exist at " + mappingsFilePath);
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(mappingsFile))) {
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            LOGGER.error("Cannot read mappings from file " + mappingsFilePath);
            mappingsFromFile = null;
        }

        if (mappingsFromFile != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Map> mappingsJson = JSONUtils.JSONStringToObject(sb.toString(), Map.class);
                for (String graphString : mappingsJson.keySet()) {
                    mappingsFromFile.put(graphString, mapper.convertValue(mappingsJson.get(graphString), QueryTemplateMapping.class));
                }
            } catch (Exception e) {
                LOGGER.error("Cannot read mappings from file " + mappingsFilePath);
                mappingsFromFile = null;
            }
        }

        this.graphToQueryTemplateMappings = mappingsFromFile;
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

    public QueryTemplateMapping getQueryTemplatesFor(String graph) {
        return this.graphToQueryTemplateMappings.get(graph);
    }

    public Collection<QueryTemplateMapping> getAllQueryTemplates() {
        return this.graphToQueryTemplateMappings.values();
    }

    public static void main(String[] args) {
        WekaClassifier w = new WekaClassifier(Dataset.LCQUAD);
        w.trainClassifier();
    }
}
