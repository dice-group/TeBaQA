package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class PattyPhrasesProvider {

    private static Configuration pattyPhrase;
    private static Logger log = Logger.getLogger(PipelineController.class);

    //do not instantiate
    private PattyPhrasesProvider() {
    }

    static Configuration getPattyPhrases() {
        return pattyPhrase;
    }

    public static void load() {
        Configuration config;
        try {
            config = new PropertiesConfiguration("patty-phrases-inverted.properties");
            PattyPhrasesProvider.pattyPhrase = config;
        } catch (ConfigurationException e) {
            log.error("Error while loading patty-phrases-inverted.properties", e);
        }
    }
}
