package de.uni.leipzig.tebaqa.controller;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.RDFNode;
import de.uni.leipzig.tebaqa.helper.NTripleParser;
import de.uni.leipzig.tebaqa.helper.QueryMappingFactory;
import de.uni.leipzig.tebaqa.helper.SPARQLUtilities;
import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import de.uni.leipzig.tebaqa.model.Cluster;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryBuilder;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import joptsimple.internal.Strings;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.measure.AnswerBasedEvaluation;
import org.apache.jena.query.QueryParseException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;


public class PipelineController {

    private static Logger log = Logger.getLogger(PipelineController.class);

    private List<Dataset> datasets = new ArrayList<>();
    private static SemanticAnalysisHelper semanticAnalysisHelper;
    static StanfordCoreNLP pipeline;
    private boolean mockTemplates = false;
    private boolean mockVariables = false;


    public static void main(String args[]) {

        log.info("Configuring controller");
        pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        semanticAnalysisHelper = new SemanticAnalysisHelper();


        PipelineController controller = new PipelineController();
        controller.addDataset(Dataset.QALD7_Train_Multilingual);

        log.info("Running controller");
        controller.run();
    }

    private void addDatasets(Dataset[] values) {
        datasets.addAll(Arrays.asList(values));
    }

    private void run() {
        log.info("Using mocked query templates: " + this.mockTemplates);
        log.info("Using mocked query variables: " + this.mockVariables);
        List<HAWKQuestion> questions = new ArrayList<>();
        for (Dataset d : datasets) {
            //Remove all questions without SPARQL query
            List<IQuestion> load = LoaderController.load(d);
            List<IQuestion> result = load.stream()
                    .filter(question -> question.getSparqlQuery() != null)
                    .collect(Collectors.toList());
            questions.addAll(HAWKQuestionFactory.createInstances(result));
        }
        HashMap<String, String> questionWithQuery = new HashMap<>();
        for (HAWKQuestion q : questions) {
            //only use unique questions in case multiple datasets are used
            String questionText = q.getLanguageToQuestion().get("en");
            if (!semanticAnalysisHelper.containsQuestionText(questionWithQuery, questionText)) {
                questionWithQuery.put(q.getSparqlQuery(), questionText);
            }
        }
        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();
        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> ontologyNodes = nTripleParser.getNodes();

        Map<String, String> idealQueryPatterns = new HashMap<>();
        Map<String, String> idealQueries = new HashMap<>();
        questionWithQuery.forEach((sparqlQuery, questionText) -> {
            String queryWithoutNS = SPARQLUtilities.resolveNamespaces(sparqlQuery);
            QueryMappingFactory queryMappingFactory = new QueryMappingFactory(questionText, queryWithoutNS, Lists.newArrayList(ontologyNodes), dBpediaProperties);
            String queryPattern = queryMappingFactory.getQueryPattern();
            idealQueryPatterns.put(questionText, queryPattern);
            idealQueries.put(questionText, queryWithoutNS);
        });

        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(questionWithQuery);
        List<Cluster> clusters = queryIsomorphism.getClusters();

        //only use clusters with at least x questions
        List<Cluster> relevantClusters = clusters.stream()
                .filter(cluster -> cluster.size() >= 0)
                .collect(Collectors.toList());
        List<CustomQuestion> customQuestions = new ArrayList<>();

        for (Cluster cluster : relevantClusters) {
            String graph = cluster.getGraph();
            //log.info(graph);
            List<Question> questionList = cluster.getQuestions();
            for (Question question : questionList) {
                String questionText = question.getLanguageToQuestion().get("en");
                //log.info("\t" + questionText);
                String sparqlQuery = question.getSparqlQuery();
                List<String> simpleModifiers = getSimpleModifiers(sparqlQuery);
                Map<String, List<String>> goldenAnswers = new HashMap<>();
                goldenAnswers.put(sparqlQuery, SPARQLUtilities.executeSPARQLQuery(sparqlQuery));
                customQuestions.add(new CustomQuestion(sparqlQuery, questionText, simpleModifiers, graph, goldenAnswers));
                semanticAnalysisHelper.annotate(questionText);
            }
        }
        QueryBuilder queryBuilder = new QueryBuilder(customQuestions, semanticAnalysisHelper);
        customQuestions = queryBuilder.getQuestions();

        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, Lists.newArrayList(ontologyNodes), dBpediaProperties);
        //log.info(mappings);
        //Utilities.writeToFile("./src/main/resources/mappings.json", mappings);


        ArffGenerator arffGenerator = new ArffGenerator(customQuestions);

        HashSet<String> graphs = new HashSet<>();
        customQuestions.forEach(customQuestion -> graphs.add(customQuestion.getGraph()));
        //Generate SPARQL Queries

        List<Double> fMeasures = new ArrayList<>();
        List<Double> precisions = new ArrayList<>();

        //TODO enable parallelization with customQuestions.parallelStream().forEach()
        customQuestions.parallelStream().forEach(question -> {
            String additionalLogMessages = "";
            String graphPattern = semanticAnalysisHelper.classifyInstance(question, graphs);
            List<String> queries;
            QueryMappingFactory mappingFactory = new QueryMappingFactory(question.getQuestionText(), question.getQuery(), Lists.newArrayList(ontologyNodes), dBpediaProperties);
            List<String> originalVariables = new ArrayList<>();
            if (mockVariables) {
                String originalQuery = idealQueries.get(question.getQuestionText());
                String regex = "<(.+?)>";
                Pattern pattern = Pattern.compile(regex);
                Matcher m = pattern.matcher(originalQuery);
                while (m.find()) {
                    originalVariables.add(m.group().replace("<", "").replace(">", ""));
                }
            }
            if (mockTemplates) {
                Map<String, QueryTemplateMapping> mockedMapping = new HashMap<>();
                QueryTemplateMapping queryTemplateMapping = new QueryTemplateMapping(0, 0);
                String originalQuery = idealQueryPatterns.get(question.getQuestionText());
                queryTemplateMapping.addSelectTemplate(originalQuery);
                queryTemplateMapping.addAskTemplate(originalQuery);
                mockedMapping.put(graphPattern, queryTemplateMapping);
                queries = mappingFactory.generateQueries(mockedMapping, graphPattern, originalVariables);
                additionalLogMessages += "Mocked Query: " + Strings.join(queries, "\n");
            } else {
                queries = mappingFactory.generateQueries(mappings, graphPattern, originalVariables);
            }
            if (queries.isEmpty()) {
                queries = mappingFactory.generateQueries(mappings);
            }

            Set<String> currentAnswers = new HashSet<>();
            for (String s : queries) {
                currentAnswers.addAll(SPARQLUtilities.executeSPARQLQuery(s));
            }
            Optional<HAWKQuestion> hawkQuestionOptional = questions.stream()
                    .filter(q -> q.getLanguageToQuestion().get("en").equals(question.getQuestionText()))
                    .findFirst();

            if (hawkQuestionOptional.isPresent()) {
                HAWKQuestion hawkQuestion = hawkQuestionOptional.get();
                if (!hawkQuestion.getAggregation() && hawkQuestion.getOnlydbo() && Objects.equals(hawkQuestion.getAnswerType(), "resource")) {
                    double fMeasure = AnswerBasedEvaluation.fMeasure(currentAnswers, hawkQuestion);
                    fMeasures.add(fMeasure);
                    double precision = AnswerBasedEvaluation.precision(currentAnswers, hawkQuestion);
                    precisions.add(precision);
                    final String[] logMessage = {String.format("Question: '%s'; F-Measure: %.3f; Precision: %.3f; Missed Entities (from golden answer):", question.getQuestionText(), fMeasure, precision)};

                    Pattern betweenLaceBraces = Pattern.compile("<(.*?)>");
                    Matcher matcher = betweenLaceBraces.matcher(SPARQLUtilities.resolveNamespaces(hawkQuestion.getSparqlQuery()));
                    Set<String> requiredDBpediaEntites = new HashSet<>();
                    while (matcher.find()) {
                        requiredDBpediaEntites.add(matcher.group().replace("<", "").replace(">", ""));
                    }
                    Set<String> rdfEntities = mappingFactory.getRdfEntities();

                    requiredDBpediaEntites.forEach(s -> {
                        if (!rdfEntities.contains(s)) {
                            logMessage[0] += s + " ";
                        }
                    });
                    log.info(logMessage[0] + "\n" + additionalLogMessages + "\n---------------------------------------------------------------------");
                }
            } else {
                log.error("Unable to get HAWK question by question text string matching!");
            }

        });
        //log.info("Correctly answered: " + correctlyAnswered[0] + "/" + questions.size());
        log.info("Average F-Measure: " + fMeasures.stream().mapToDouble(Double::doubleValue).summaryStatistics().getAverage());
        log.info("Average Precision: " + precisions.stream().mapToDouble(Double::doubleValue).summaryStatistics().getAverage());

    }

    private void addUnresolvedEntities(Map<String, List<String>> from, Map<String, Map<String, Integer>> to) {
        from.forEach((entity, posSequence) -> {
            if (to.containsKey(entity)) {
                Map<String, Integer> tmp = to.get(entity);
                posSequence.forEach(s -> {
                    if (tmp.containsKey(s)) {
                        tmp.put(s, tmp.get(s) + 1);
                    } else {
                        tmp.put(s, 1);
                    }
                });
                to.put(entity, tmp);
            } else {
                HashMap<String, Integer> tmp = new HashMap<>();
                posSequence.forEach(s -> tmp.put(s, 1));
                to.put(entity, tmp);
            }
        });
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

    private void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    private void addDataset(Dataset dataset) {
        this.datasets.add(dataset);
    }
}
