package de.uni.leipzig.tebaqa.helper;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;

public class StanfordPipelineProvider {

    private static StanfordCoreNLP pipeline;

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
            pipeline = new StanfordCoreNLP(PropertiesUtils.asProperties(
                    "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,depparse,ner",
                    "ssplit.isOneSentence", "true",
                    "tokenize.language", "en"));
        }
        return pipeline;
    }
}
