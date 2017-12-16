package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class HypernymMappingProvider {

    private static Map<String, List<String>> hypernymMapping;
    private static Logger log = Logger.getLogger(PipelineController.class);


    //do not instantiate
    private HypernymMappingProvider() {
    }

    public static Map<String, List<String>> getHypernymMapping() {
        if (null == hypernymMapping) {
            log.info("Load hypernym mapping...");
            try {
                hypernymMapping = loadHypernymMappings();
            } catch (IOException e) {
                log.error("Unable to load ontology mapping from file: hypernyms.properties!", e);
            }
        }
        return hypernymMapping;
    }

    private static Map<String, List<String>> loadHypernymMappings() throws IOException {
        Map<String, List<String>> mapping = new HashMap<>();
        Properties properties = new Properties();
        properties.load(new FileInputStream(new ClassPathResource("hypernyms.properties").getFile()));

        for (String key : properties.stringPropertyNames()) {
            mapping.put(key, Arrays.asList(properties.get(key).toString().split(";")));
        }
        return mapping;
    }
}
