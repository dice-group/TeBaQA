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

import java.util.List;
import java.util.Map;

public class PipelineController {
    private static Logger log = Logger.getLogger(PipelineController.class);

    private Dataset dataset;
    private StanfordCoreNLP pipeline;

    public static void main(String args[]) {
        PipelineController controller = new PipelineController();
        log.info("Configuring controller");

        Dataset dataset = Dataset.QALD1_Test_dbpedia;
        controller.setDataset(dataset);
        log.info("Dataset: " + dataset);

        controller.setStanfordNLPPipeline(new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,parse,natlog",
                        "ssplit.isOneSentence", "true",
                        "tokenize.language", "en")));

        log.info("Running controller");
        controller.run();
    }

    private void run() {
        List<IQuestion> load = LoaderController.load(dataset);
        List<HAWKQuestion> questions = HAWKQuestionFactory.createInstances(load);
        Fox fox = new Fox();
        for (HAWKQuestion q : questions) {
            String questionText = q.getLanguageToQuestion().get("en");
            log.info(questionText);
            Map<String, List<Entity>> entities = fox.getEntities(questionText);
            log.info("Entities from FOX:" + entities);
            Annotation document = new Annotation(questionText);
            pipeline.annotate(document);
            log.info("Annotated Document: " + document);

            Document doc = new Document(questionText);
            for (Sentence sent : doc.sentences()) {
                List<String> parse = sent.posTags();
                log.info("The parse of the sentence '" + sent + "' is " + parse);
                List<String> words = sent.words();
                for (int i = 0; i < words.size(); i++) {
                    log.info(String.format("Word: '%s' is %s", words.get(i), parse.get(i)));
                }
            }
        }
    }

    private void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    private void setStanfordNLPPipeline(StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }

}
