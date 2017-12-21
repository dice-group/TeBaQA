package de.uni.leipzig.tebaqa.spring;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.SimpleLayout;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import javax.annotation.PostConstruct;
import java.io.IOException;

import static de.uni.leipzig.tebaqa.helper.HypernymMappingProvider.getHypernymMapping;
import static de.uni.leipzig.tebaqa.helper.PipelineProvider.getQAPipeline;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    @PostConstruct
    public void init() {
        getQAPipeline();
        Logger logger = Logger.getRootLogger();
        getHypernymMapping();
        logger.info("Initialisation of TeBaQA finished.");
    }

    public static void main(String[] args) throws IOException {
        configureLogger();
        SpringApplication.run(Application.class, args);
    }

    private static void configureLogger() throws IOException {
        EnhancedPatternLayout enhancedPatternLayout = new EnhancedPatternLayout();
        enhancedPatternLayout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");
        ConsoleAppender consoleAppender = new ConsoleAppender(enhancedPatternLayout);
        RollingFileAppender fileAppender = new RollingFileAppender(new SimpleLayout(), "log.out", true);
        fileAppender.setMaxFileSize("500MB");
        Logger logger = Logger.getRootLogger();
        logger.removeAllAppenders();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(consoleAppender);
        logger.addAppender(fileAppender);
        //org.apache.log4j.BasicConfigurator.configure();
    }
}
