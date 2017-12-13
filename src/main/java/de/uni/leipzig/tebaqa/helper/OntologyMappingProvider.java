package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class OntologyMappingProvider {

    private static Map<String, List<String>> ontologyMapping;
    private static Logger log = Logger.getLogger(PipelineController.class);


    //do not instantiate
    private OntologyMappingProvider() {
    }

    /**
     * Provides a singleton instance of the StanfordCoreNLP pipeline.
     *
     * @return A shared instance of the StanfordCoreNLP pipeline.
     */
    public static Map<String, List<String>> getOntologyMapping() {
        //disables logging messages from stanford
        RedwoodConfiguration.current().clear().apply();

        if (null == ontologyMapping) {
            log.info("Load ontology mapping...");
            try {
                ontologyMapping = loadOntologyMapping();
            } catch (IOException e) {
                log.error("Unable to load ontology mapping from file: ontology-mappings.properties!", e);
            }
        }
        return ontologyMapping;
    }

    private static Map<String, List<String>> loadOntologyMapping() throws IOException {
        Map<String, List<String>> ldapContent = new HashMap<>();
        Properties properties = new Properties();
        //properties.load(new FileInputStream("./src/main/resources/ontology-mappings.properties"));
        properties.load(new FileInputStream(new ClassPathResource("ontology-mappings.properties").getFile()));

        for (String key : properties.stringPropertyNames()) {
            ldapContent.put(key, Arrays.asList(properties.get(key).toString().split(",")));
        }
        return ldapContent;
    }
}
