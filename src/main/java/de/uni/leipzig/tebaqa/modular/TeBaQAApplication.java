package de.uni.leipzig.tebaqa.modular;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class TeBaQAApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(TeBaQAApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TeBaQAApplication.class);
    }

    @PostConstruct
    public void init() {
//        getQAPipeline();
//        //getQAPipelineTripleTemplates();
//        Logger logger = Logger.getRootLogger();
//        getHypernymMapping();
//        logger.info("Initialisation of TeBaQA finished.");
    }
}
