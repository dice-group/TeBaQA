package de.uni.leipzig.tebaqa.helper;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class StanfordPipelineProviderGerman {
    private static StanfordCoreNLP pipeline;
    private static Logger log = Logger.getLogger(StanfordPipelineProvider.class);


    //do not instantiate
    private StanfordPipelineProviderGerman() {
    }

    /**
     * Provides a singleton instance of the StanfordCoreNLP pipeline.
     *
     * @return A shared instance of the StanfordCoreNLP pipeline.
     */
    public static StanfordCoreNLP getSingletonPipelineInstance() {
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

        pipeline= new StanfordCoreNLP(props);
        return pipeline;
    }
}
