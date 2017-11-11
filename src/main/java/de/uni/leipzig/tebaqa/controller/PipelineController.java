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
import org.apache.jena.query.QueryParseException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        List<HAWKQuestion> questions = new ArrayList<>();
        for (Dataset d : datasets) {
            //Filter all questions without SPARQL query
            List<IQuestion> load = LoaderController.load(d);
            List<IQuestion> result = load.stream()
                    .filter(question -> question.getSparqlQuery() != null)
                    .collect(Collectors.toList());
            questions.addAll(HAWKQuestionFactory.createInstances(result));
        }
        HashMap<String, String> questionWithQuery = new HashMap<>();
        for (HAWKQuestion q : questions) {
            //only use unique questions
            String questionText = q.getLanguageToQuestion().get("en");
            if (!semanticAnalysisHelper.containsQuestionText(questionWithQuery, questionText)) {
                questionWithQuery.put(q.getSparqlQuery(), questionText);
            }
        }

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

        NTripleParser nTripleParser = new NTripleParser();
        Set<RDFNode> nodes = nTripleParser.getNodes();

        List<String> dBpediaProperties = SPARQLUtilities.getDBpediaProperties();

        Map<String, QueryTemplateMapping> mappings = semanticAnalysisHelper.extractTemplates(customQuestions, Lists.newArrayList(nodes), dBpediaProperties);
        //log.info(mappings);
        //Utilities.writeToFile("./src/main/resources/mappings.json", mappings);


        ArffGenerator arffGenerator = new ArffGenerator(customQuestions);

        List<Map<String, List<String>>> answers = new ArrayList<>();
        Map<String, List<String>> questionToAnswers = new HashMap<>();
        HashSet<String> graphs = new HashSet<>();
        customQuestions.forEach(customQuestion -> graphs.add(customQuestion.getGraph()));
        final int[] correctlyAnswered = {0};
        //Generate SPARQL Queries

        //TODO enable parallelization with customQuestions.parallelStream.forEach()
        customQuestions.forEach(question -> {
            boolean answered = false;
            String graphPattern = semanticAnalysisHelper.classifyInstance(question, graphs);

            QueryMappingFactory mappingFactory = new QueryMappingFactory(question.getQuestionText(), question.getQuery(), Lists.newArrayList(nodes), dBpediaProperties);
            List<String> queries = mappingFactory.generateQueries(mappings, graphPattern);
            if (queries.isEmpty()) {
                queries = mappingFactory.generateQueries(mappings);
            }

            Map<String, List<String>> goldenAnswers = question.getGoldenAnswers();
            List<String> correctAnswers = new ArrayList<>();
            goldenAnswers.forEach((s, strings) -> {
                //log.info(String.format("Golden Answer: %s", Strings.join(strings, "; ")));
                correctAnswers.addAll(strings);
            });
            List<String> currentAnswers = new ArrayList<>();
            queryIteration:
            for (String s : queries) {
                List<String> tmp = SPARQLUtilities.executeSPARQLQuery(s);
                currentAnswers.addAll(tmp);
                if (correctAnswers.containsAll(tmp) && tmp.containsAll(correctAnswers) && !tmp.isEmpty()) {
                    correctlyAnswered[0]++;
                    //TODO Calculate f-measure instead of counting correct answers!
                    log.info(String.format("Found correct answer! Question: '%s'\nAnswer(s): '%s'\nQuery: %s", question.getQuestionText(), Strings.join(tmp, ";"), s));
                    answered = true;
                    break queryIteration;
                }
            }
            if (!answered) {
                log.info("Unanswered: " + question.getQuestionText());
                log.info("Correct would be: " + question.getQuery());
            }
            questionToAnswers.put(question.getQuestionText(), currentAnswers);
            answers.add(questionToAnswers);
            log.info("---------------------------------------------------------------------------------------------------------------");
        });
        log.info("Correctly answered: " + correctlyAnswered[0] + "/" + questions.size());
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
