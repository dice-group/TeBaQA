package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

    public enum Lang {
        EN() {
            @Override
            public StanfordCoreNLP getPipeline() {
                //disables logging messages from stanford
                RedwoodConfiguration.current().clear().apply();

                LOGGER.info("Creating StanfordCoreNLP " + this.name() + " pipeline...");
                StanfordCoreNLP instance = new StanfordCoreNLP(PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,ner",
//                    "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,depparse,ner",
                        "ssplit.isOneSentence", "true",
                        "tokenize.language", "en"));
                return instance;
            }
        },
        DE() {
            @Override
            public StanfordCoreNLP getPipeline() {
                //disables logging messages from stanford
                RedwoodConfiguration.current().clear().apply();

                Properties props = new Properties();
                try {
                    props.load(IOUtils.readerFromString("StanfordCoreNLP-german.properties"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //props.remove("annotators");
                //props.setProperty("annotators","tokenize,ssplit,pos,ner,parse,depparse");

                return new StanfordCoreNLP(props);
            }
        };

        public abstract StanfordCoreNLP getPipeline();
    }
}
