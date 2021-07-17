package de.uni.leipzig.tebaqa.queryranking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class QueryRankingApplication {

    public static Logger LOGGER = LogManager.getRootLogger();

    public static void main(String[] args) {
        LOGGER.info("Query ranking linking server started");
        SpringApplication.run(QueryRankingApplication.class, args);
    }

    @PostConstruct
    public void init() {
        LOGGER.info("Query ranking server started");
    }
}
