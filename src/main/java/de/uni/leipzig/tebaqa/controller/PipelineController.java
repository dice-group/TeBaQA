package de.uni.leipzig.tebaqa.controller;

import de.uni.leipzig.tebaqa.helper.QueryMapping;
import de.uni.leipzig.tebaqa.helper.Utilities;
import de.uni.leipzig.tebaqa.model.Cluster;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryBuilder;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PipelineController {
    private static Logger log = Logger.getLogger(PipelineController.class);

    private List<Dataset> datasets = new ArrayList<>();
    private StanfordCoreNLP pipeline;
    private SemanticAnalysisHelper semanticAnalysisHelper = new SemanticAnalysisHelper();

    public static void main(String args[]) {
        PipelineController controller = new PipelineController();
        log.info("Configuring controller");

        controller.addDataset(Dataset.QALD7_Train_Multilingual);

        controller.setStanfordNLPPipeline(new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,depparse",
                        "ssplit.isOneSentence", "true",
                        "tokenize.language", "en")));

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
            if (!containsQuestionText(questionWithQuery, questionText)) {
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
                List<String> simpleModifiers = getSimpleModifiers(question.getSparqlQuery());
                customQuestions.add(new CustomQuestion(question.getSparqlQuery(), questionText, simpleModifiers, graph));
                semanticAnalysisHelper.annotate(questionText);
            }
        }
        QueryBuilder queryBuilder = new QueryBuilder(customQuestions);
        customQuestions = queryBuilder.getQuestions();

        Map<String, Map<String, Integer>> unresolvedEntities = new HashMap<>();
        Map<String, List<String>> mappings = new HashMap<>();
        int completeMappings = 0;
        for (CustomQuestion question : customQuestions) {
            QueryMapping queryMapping = new QueryMapping(question.getQuestionText(),
                    question.getDependencySequencePosMap(), question.getQuery());
            String queryPattern = queryMapping.getQueryPattern();

            Map<String, List<String>> unresolved = queryMapping.getUnresolvedEntities();
            addUnresolvedEntities(unresolved, unresolvedEntities);


            if (!queryPattern.contains("http://")) {
                completeMappings++;
                if (mappings.containsKey(question.getGraph())) {
                    List<String> currMappingsOfGraph = mappings.get(question.getGraph());
                    currMappingsOfGraph.add(queryPattern);
                    mappings.put(question.getGraph(), currMappingsOfGraph);
                } else {
                    mappings.put(question.getGraph(), new ArrayList<>(Collections.singletonList(queryPattern)));
                }

                //log.info(question.getGraph());
                //log.info(queryPattern);
            } else {
                log.info(queryPattern + "\n" + question.getQuestionText() + "\n" + question.getDependencySequencePosMap());
                log.info("\n");
            }
            //log.info(question.toString() + "\n" + queryPattern);
            //log.info("---------------------------------------------------------------\n");
        }
        log.info("Got " + completeMappings + " / " + customQuestions.size() + " Mappings.");
        log.info(mappings);
        Utilities.writeToFile("./src/main/resources/mappings.json", mappings);

        ArffGenerator arffGenerator = new ArffGenerator(customQuestions);

        //QueryTemplatesBuilder templatesBuilder = new QueryTemplatesBuilder(sparqlQueries);
        //List<QueryTemplate> queryTemplates = templatesBuilder.getQueryTemplates();
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
            String trimmedQuery = cleanQuery(queryString);

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
        return Collections.emptyList();
    }

    /**
     * Removes all variables, prefixes, newlines, standard keywords like ASK, SELECT, WHERE, DISTINCT.
     *
     * @param queryString The sparql query string.
     * @return A string which only contains sparql modifiers, a '?' as placeholder for a variable and '<>' as
     * placeholders for strings like this: { <> <> ? . ? <> ? FILTER regex( ? , ? ) }
     */
    private String cleanQuery(String queryString) {
        Query query = QueryFactory.create(queryString);
        query.setPrefixMapping(null);
        return query.toString().trim()
                //replace newlines with space
                .replaceAll("\n", " ")
                //replace every variable with ?
                .replaceAll("\\?[a-zA-Z\\d]+", " ? ")
                //replace every number(e.g. 2 or 2.5) with a ?
                .replaceAll("\\s+\\d+\\.?\\d*", " ? ")
                //replace everything in quotes with ?
                .replaceAll("([\"'])(?:(?=(\\\\?))\\2.)*?\\1", " ? ")
                //remove everything between <>
                .replaceAll("<\\S*>", " <> ")
                //remove all SELECT, ASK, DISTINCT and WHERE keywords
                .replaceAll("(?i)(select|ask|where|distinct)", " ")
                //remove every consecutive spaces
                .replaceAll("\\s+", " ");
    }

    /**
     * Checks, if a question is inside a Map.
     *
     * @param map  The map in which the question is not is not.
     * @param text The question text.
     * @return true if the text is inside, false otherwise.
     */
    private boolean containsQuestionText(Map<String, String> map, String text) {
        boolean isInside = false;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(text)) {
                isInside = true;
                break;
            }
        }
        return isInside;
    }

    private void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    private void addDataset(Dataset dataset) {
        this.datasets.add(dataset);
    }

    private void setStanfordNLPPipeline(StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }
}
