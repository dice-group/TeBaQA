package de.uni.leipzig.tebaqa.tebaqacontroller;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.StanfordPipelineProvider;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TebaqaControllerApplication {

	public static Logger LOGGER = Logger.getRootLogger();

	public static void main(String[] args) {
        LOGGER.info("Starting TeBaQA controller ...");
        StanfordPipelineProvider.getSingletonPipelineInstance(Lang.EN);
        SpringApplication.run(TebaqaControllerApplication.class, args);
    }

}
