package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class StanfordPipelineProvider {
    private static final Logger LOGGER = Logger.getLogger(StanfordPipelineProvider.class);
    private static final Map<Lang, StanfordCoreNLP> pipelineInstances = new HashMap<>(Lang.values().length);

    //do not instantiate
    private StanfordPipelineProvider() {
    }

    /**
     * Provides a singleton instance of the StanfordCoreNLP pipeline.
     *
     * @return A shared instance of the StanfordCoreNLP pipeline.
     */
    public static StanfordCoreNLP getSingletonPipelineInstance(Lang lang) {
        StanfordCoreNLP instance = pipelineInstances.get(lang);
        if (null == instance) {
            instance = getPipeline();
            pipelineInstances.put(lang, instance);
        }
        return instance;
    }

    public static StanfordCoreNLP getPipeline() {
        //disables logging messages from stanford
        RedwoodConfiguration.current().clear().apply();

        LOGGER.info("Creating StanfordCoreNLP pipeline...");
        StanfordCoreNLP instance = new StanfordCoreNLP(PropertiesUtils.asProperties(
                "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,ner",
//                    "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,depparse,ner",
                "ssplit.isOneSentence", "true",
                "tokenize.language", "en"));
        return instance;
    }


}
