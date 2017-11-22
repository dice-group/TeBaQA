package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import org.apache.log4j.Logger;

public class StanfordPipelineProvider {

    private static StanfordCoreNLP pipeline;
    private static Logger log = Logger.getLogger(PipelineController.class);


    //do not instantiate
    private StanfordPipelineProvider() {
    }

    /**
     * Provides a singleton instance of the StanfordCoreNLP pipeline.
     *
     * @return A shared instance of the StanfordCoreNLP pipeline.
     */
    public static StanfordCoreNLP getSingletonPipelineInstance() {
        //disables logging messages from stanford
        RedwoodConfiguration.current().clear().apply();

        if (null == pipeline) {
            log.info("Creating StanfordCoreNLP pipeline...");
            pipeline = new StanfordCoreNLP(PropertiesUtils.asProperties(
                    "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,depparse,ner",
                    "ssplit.isOneSentence", "true",
                    "tokenize.language", "en"));
        }
        return pipeline;
    }
}
