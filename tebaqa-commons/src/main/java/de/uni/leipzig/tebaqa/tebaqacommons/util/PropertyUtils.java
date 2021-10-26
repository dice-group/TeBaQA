package de.uni.leipzig.tebaqa.tebaqacommons.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class PropertyUtils {

    private static final Logger LOGGER = LogManager.getLogger(PropertyUtils.class.getName());

    public static final String CLASS_INDEX_SUFFIX = "-class";
    public static final String PROPERTY_INDEX_SUFFIX = "-property";
    public static final String ENTITY_INDEX_SUFFIX = "-entity";

    public static Properties getAllProperties(String propertyFilePath) {

        Properties prop = new Properties();
        InputStream input;
        try {
            input = PropertyUtils.class.getClassLoader().getResourceAsStream(propertyFilePath);
            prop.load(input);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return prop;
    }

    public static String getESIndexNamePrefix(String input) {
        return input.trim().toLowerCase().replaceAll("\\s+", "");
    }

    public static String getESClassIndexName(String input) {
        return getESIndexNamePrefix(input) + CLASS_INDEX_SUFFIX;
    }

    public static String getESPropertyIndexName(String input) {
        return getESIndexNamePrefix(input) + PROPERTY_INDEX_SUFFIX;
    }

    public static String getESEntityIndexName(String input) {
        return getESIndexNamePrefix(input) + ENTITY_INDEX_SUFFIX;
    }

}
