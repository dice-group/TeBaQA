package de.uni.leipzig.tebaqa.controller;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineController {
    private static Logger log = Logger.getLogger(PipelineController.class);

    private List<Dataset> datasets = new ArrayList<Dataset>();
    private StanfordCoreNLP pipeline;

    public static void main(String args[]) {
        PipelineController controller = new PipelineController();
        log.info("Configuring controller");

        controller.addDataset(Dataset.QALD6_Test_Multilingual);
        controller.addDataset(Dataset.QALD7_Train_Multilingual);

        controller.setStanfordNLPPipeline(new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,parse,natlog",
                        "ssplit.isOneSentence", "true",
                        "tokenize.language", "en")));

        log.info("Running controller");
        controller.run();
    }

    private void run() {
        List<HAWKQuestion> questions = new ArrayList<HAWKQuestion>();
        for (Dataset d : datasets) {
            //Filter all monolingual questions without SPARQL query
            List<IQuestion> load = LoaderController.load(d);
            List<IQuestion> result = load.stream()
                    .filter(question -> question.getLanguageToQuestion().size() > 2 && question.getSparqlQuery() != null)
                    .collect(Collectors.toList());
            questions.addAll(HAWKQuestionFactory.createInstances(result));
        }
        Fox fox = new Fox();
        HashMap<String, String> questionWithQuery = new HashMap<String, String>();
        for (HAWKQuestion q : questions) {
            String questionText = q.getLanguageToQuestion().get("en");
            questionWithQuery.put(q.getSparqlQuery(), questionText);
            log.info(questionText);
            Map<String, List<Entity>> entities = fox.getEntities(questionText);
            log.info("Entities from FOX:" + entities);
            Annotation document = new Annotation(questionText);
            pipeline.annotate(document);

            Document doc = new Document(questionText);
            for (Sentence sent : doc.sentences()) {
                List<String> parse = sent.posTags();
                //log.info("The parse of the sentence '" + sent + "' is " + parse);
            }
        }
        QueryIsomorphism queryIsomorphism = new QueryIsomorphism(questionWithQuery);
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
