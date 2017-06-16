package de.uni.leipzig.tebaqa.controller;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.annotation.spotter.Fox;
import org.aksw.qa.commons.datastructure.Entity;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineController {
    private static Logger log = Logger.getLogger(PipelineController.class);

    private List<Dataset> datasets = new ArrayList<>();
    private StanfordCoreNLP pipeline;

    public static void main(String args[]) {
        PipelineController controller = new PipelineController();
        log.info("Configuring controller");

        controller.addDatasets(Dataset.values());

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

    private void annotate(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        pipeline.prettyPrint(annotation, System.out);
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
        Fox fox = new Fox();
        HashMap<String, String> questionWithQuery = new HashMap<>();
        for (HAWKQuestion q : questions) {
            String questionText = q.getLanguageToQuestion().get("en");
            if (!containsQuestionText(questionWithQuery, questionText)) {
                questionWithQuery.put(q.getSparqlQuery(), questionText);
            } else {
                log.info("Duplicate question: " + questionText);
            }
            log.info(questionText);
            Map<String, List<Entity>> entities = fox.getEntities(questionText);
            log.info("Entities from FOX:" + entities);
            annotate(questionText);
        }
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(questionWithQuery);
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
