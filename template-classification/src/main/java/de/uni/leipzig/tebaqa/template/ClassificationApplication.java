package de.uni.leipzig.tebaqa.template;

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

        SpringApplication.run(ClassificationApplication.class, args);
    }
}
