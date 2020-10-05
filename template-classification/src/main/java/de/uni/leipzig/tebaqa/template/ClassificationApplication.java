package de.uni.leipzig.tebaqa.template;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClassificationApplication {

    public static Logger LOGGER = Logger.getRootLogger();


    public static void main(String[] args) {
        LOGGER.info("Template classification server started");


        SpringApplication.run(ClassificationApplication.class, args);
    }
}
