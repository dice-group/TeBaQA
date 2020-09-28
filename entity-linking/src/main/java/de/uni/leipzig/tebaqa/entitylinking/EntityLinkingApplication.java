package de.uni.leipzig.tebaqa.entitylinking;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class EntityLinkingApplication {

    public static Logger LOGGER = Logger.getRootLogger();


    public static void main(String[] args) {
        LOGGER.info("Entity linking server started");
        SpringApplication.run(EntityLinkingApplication.class, args);
    }

    @PostConstruct
    public void init() {
        LOGGER.info("Entity linking server started");
    }
}
