package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
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
            instance = lang.getPipeline();
            pipelineInstances.put(lang, instance);
        }
        return instance;
    }


}
