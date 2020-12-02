package de.uni.leipzig.tebaqa.spring;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
//import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import javax.annotation.PostConstruct;

import static de.uni.leipzig.tebaqa.helper.HypernymMappingProvider.getHypernymMapping;
import static de.uni.leipzig.tebaqa.helper.PipelineProvider.getQAPipeline;
import static de.uni.leipzig.tebaqa.helper.PipelineProvider.getQAPipelineTripleTemplates;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    @PostConstruct
    public void init() {
        getQAPipeline();
        //getQAPipelineTripleTemplates();
        Logger logger = Logger.getRootLogger();
        getHypernymMapping();
        logger.info("Initialisation of TeBaQA finished.");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
