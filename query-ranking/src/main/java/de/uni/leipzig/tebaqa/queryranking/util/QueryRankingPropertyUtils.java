package de.uni.leipzig.tebaqa.queryranking.util;

import de.uni.leipzig.tebaqa.tebaqacommons.model.ESConnectionProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class QueryRankingPropertyUtils {

    private static final Logger LOGGER = LogManager.getLogger(QueryRankingPropertyUtils.class);

    private static ESConnectionProperties ES_CONNECTION_PROPERTIES = null;

    public static ESConnectionProperties getElasticSearchConnectionProperties() {

        if (ES_CONNECTION_PROPERTIES == null) {

            Properties prop = new Properties();
            InputStream input;
            try {

                input = new FileInputStream("src/main/resources/elasticsearch.properties");
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
}
