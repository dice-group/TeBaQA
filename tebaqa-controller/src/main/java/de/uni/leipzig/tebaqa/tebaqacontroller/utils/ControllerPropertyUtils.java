package de.uni.leipzig.tebaqa.tebaqacontroller.utils;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ControllerPropertyUtils {

    private static final Logger LOGGER = Logger.getLogger(ControllerPropertyUtils.class.getName());
    private static Properties ALL_PROPERTIES;
    private static String CLASSIFICATION_SERVICE_URL = null;
    private static String LINKING_SERVICE_URL = null;
    private static String RANKING_SERVICE_URL = null;

    static {
        ALL_PROPERTIES = getAllProperties();
    }

    public ControllerPropertyUtils() {
        ALL_PROPERTIES = getAllProperties();
    }

    private static Properties getAllProperties() {

        Properties prop = new Properties();
        InputStream input;
        try {
            input = ControllerPropertyUtils.class.getClassLoader().getResourceAsStream("application.properties");
            prop.load(input);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return prop;
    }

    public static String getClassificationServiceUrl() {
        if (CLASSIFICATION_SERVICE_URL == null) {

            String host = ALL_PROPERTIES.getProperty("template.classification.host");
            String port = ALL_PROPERTIES.getProperty("template.classification.port");
            String endpoint = ALL_PROPERTIES.getProperty("template.classification.endpoint");

            CLASSIFICATION_SERVICE_URL = String.format("%s:%s/%s", host, port, endpoint);
        }
        return CLASSIFICATION_SERVICE_URL;
    }

    public static String getEntityLinkingServiceUrl() {
        if (LINKING_SERVICE_URL == null) {

            String host = ALL_PROPERTIES.getProperty("el.host");
            String port = ALL_PROPERTIES.getProperty("el.port");
            String endpoint = ALL_PROPERTIES.getProperty("el.endpoint");

            LINKING_SERVICE_URL = String.format("%s:%s/%s", host, port, endpoint);
        }
        return LINKING_SERVICE_URL;
    }

    public static String getQueryRankingServiceUrl() {
        if (RANKING_SERVICE_URL == null) {

            String host = ALL_PROPERTIES.getProperty("query.ranking.host");
            String port = ALL_PROPERTIES.getProperty("query.ranking.port");
            String endpoint = ALL_PROPERTIES.getProperty("query.ranking.endpoint");

            RANKING_SERVICE_URL = String.format("%s:%s/%s", host, port, endpoint);
        }
        return RANKING_SERVICE_URL;
    }
}
