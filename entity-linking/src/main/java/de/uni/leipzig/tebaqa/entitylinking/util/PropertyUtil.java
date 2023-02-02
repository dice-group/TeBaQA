package de.uni.leipzig.tebaqa.entitylinking.util;

import de.uni.leipzig.tebaqa.tebaqacommons.model.ESConnectionProperties;
import de.uni.leipzig.tebaqa.tebaqacommons.model.RestServiceConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
public class PropertyUtil {

    private static final Logger LOGGER = LogManager.getLogger(PropertyUtil.class);

    private static ESConnectionProperties ES_CONNECTION_PROPERTIES = null;
    private static RestServiceConfiguration NLP_CONNECTION_PROPERTIES = null;

    private static String FILE_NAME;

    @Value("${spring.application.entitylinking.properties}")
    public void setFileName(String name){
        this.FILE_NAME = name;
    }
    public static ESConnectionProperties getElasticSearchConnectionProperties() {
        if (ES_CONNECTION_PROPERTIES == null) {

            Properties prop = new Properties();
            InputStream input;
            try {
                input = PropertyUtil.class.getResourceAsStream(FILE_NAME);
                prop.load(input);
                String hostname = prop.getProperty("el_hostname");
                String port = prop.getProperty("el_port");
                String scheme = prop.getProperty("scheme");
                String resourceIndex = prop.getProperty("resource_index");
                String propertyIndex = prop.getProperty("property_index");
                String classIndex = prop.getProperty("class_index");
                String literalIndex = prop.getProperty("literal_index");

                ES_CONNECTION_PROPERTIES = new ESConnectionProperties(scheme, hostname, port, resourceIndex, classIndex, propertyIndex, literalIndex);

            } catch (IOException e) {
                LOGGER.error("Cannot read entity linking properties file: " + e.getMessage());
            }
        }
        return ES_CONNECTION_PROPERTIES;
    }

    public static RestServiceConfiguration getNLPServiceConnectionProperties() {
        if (NLP_CONNECTION_PROPERTIES == null) {

            Properties prop = new Properties();
            InputStream input;
            try {
                input = new FileInputStream("src/main/resources/nlp.properties");
                prop.load(input);
                String hostname = prop.getProperty("service.nlp.host");
                String port = prop.getProperty("service.nlp.port");
                String scheme = prop.getProperty("service.nlp.scheme");

                NLP_CONNECTION_PROPERTIES = new RestServiceConfiguration(scheme, hostname, port);

            } catch (IOException e) {
                LOGGER.error("Cannot read nlp properties file: " + e.getMessage());
            }
        }
        return NLP_CONNECTION_PROPERTIES;
    }
}
