package de.uni.leipzig.tebaqa.template;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.StanfordPipelineProvider;
import de.uni.leipzig.tebaqa.template.service.WekaClassifier;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
public class ClassificationApplication {

    public static Logger LOGGER = Logger.getRootLogger();


    public static void main(String[] args) throws IOException {
        LOGGER.info("Template classification server started");

        // Load properties
        Properties p = new Properties();
        p.load(new ClassPathResource("template-classification.properties").getInputStream());
        p.load(new ClassPathResource("application.properties").getInputStream());
        System.getProperties().putAll(p);

        // Load Stanford NLP pipeline at start up
        StanfordPipelineProvider.getSingletonPipelineInstance(StanfordPipelineProvider.Lang.EN);

        // Prepare Weka classifier model at start up
        WekaClassifier.getDefaultClassifier();

        SpringApplication.run(ClassificationApplication.class, args);
    }
}
