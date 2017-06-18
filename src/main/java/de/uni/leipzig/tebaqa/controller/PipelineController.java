package de.uni.leipzig.tebaqa.controller;

import de.uni.leipzig.tebaqa.model.Cluster;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.annotation.spotter.Fox;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class PipelineController {
    private static Logger log = Logger.getLogger(PipelineController.class);

    private List<Dataset> datasets = new ArrayList<>();
    private StanfordCoreNLP pipeline;

    public static void main(String args[]) {
        PipelineController controller = new PipelineController();
        log.info("Configuring controller");

        controller.addDatasets(Dataset.values());

        //TODO reanenable before commiting
        /*controller.setStanfordNLPPipeline(new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,depparse",
                        "ssplit.isOneSentence", "true",
                        "tokenize.language", "en")));
         */
        log.info("Running controller");
        controller.run();
    }

    private void addDatasets(Dataset[] values) {
        datasets.addAll(Arrays.asList(values));
    }

    private Annotation annotate(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        pipeline.prettyPrint(annotation, System.out);
        return annotation;
    }

    private void run() {
        List<HAWKQuestion> questions = new ArrayList<>();
        List<String> sparqlQueries = new ArrayList<>();
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
            String questionText = q.getLanguageToQuestion().get("en");
            if (!containsQuestionText(questionWithQuery, questionText)) {
                questionWithQuery.put(q.getSparqlQuery(), questionText);
                sparqlQueries.add(q.getSparqlQuery());
            } else {
                log.info("Duplicate question: " + questionText);
            }
            log.info(questionText);
        }

        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(questionWithQuery);
        List<Cluster> clusters = queryIsomorphism.getClusters();
        List<Cluster> relevantClusters = clusters.stream()
                .filter(cluster -> cluster.size() > 10)
                .collect(Collectors.toList());
        log.info("Create SPARQL Queries from questions");
        new QueryTemplatesBuilder(sparqlQueries);

    }

    private void createQueries(List<Cluster> clusters) {
        // TODO Query aus Frage erstellen
        // createQueries(relevantClusters);
        Fox fox = new Fox();
        for (Cluster cluster : clusters) {
            List<Question> questions = cluster.getQuestions();
            for (Question question : questions) {
                String sparqlQuery = "";
                String questionText = question.getLanguageToQuestion().get("en");
                //Map<String, List<Entity>> entities = fox.getEntities(questionText);
                //log.info("Entities from FOX:" + entities);
                Annotation annotation = annotate(questionText);
                // TODO anhand der Annotation zwischen ASK und SELECT query unterscheiden
                // Wenn an erster Stelle VBZ kommt -> ASK, sonst SELECT
                // Siehe pipeline.prettyPrint(), woher PartOfSpeech=VBZ kommt
                List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
                CoreMap sentence = sentences.get(0);
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                String posTag = tokens.get(0).tag();
                System.out.println(posTag);
                if (posTag.equals("VBZ")) {
                    sparqlQuery = "ASK WHERE ";
                } else {
                    sparqlQuery = "SELECT DISTINCT ";
                }
            }
        }
    }

    private boolean containsQuestionText(HashMap<String, String> map, String text) {
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
