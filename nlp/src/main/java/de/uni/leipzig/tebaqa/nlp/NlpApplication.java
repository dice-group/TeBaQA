package de.uni.leipzig.tebaqa.nlp;

import de.uni.leipzig.tebaqa.nlp.core.NLPLang;
import de.uni.leipzig.tebaqa.nlp.core.StanfordPipelineProvider;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NlpApplication {

	public static void main(String[] args) {
		Logger rootLogger = Logger.getRootLogger();
		rootLogger.info("Starting NPL application ...");

		// init
//		StanfordPipelineProvider.getSingletonPipelineInstance(NLPLang.EN);

		SpringApplication.run(NlpApplication.class, args);
	}

}
