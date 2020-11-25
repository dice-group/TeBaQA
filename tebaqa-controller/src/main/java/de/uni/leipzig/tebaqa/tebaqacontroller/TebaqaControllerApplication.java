package de.uni.leipzig.tebaqa.tebaqacontroller;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TebaqaControllerApplication {

    public static Logger LOGGER = Logger.getRootLogger();

    public static void main(String[] args) {
        LOGGER.info("Starting TeBaQA controller ...");
        SpringApplication.run(TebaqaControllerApplication.class, args);
    }

}
