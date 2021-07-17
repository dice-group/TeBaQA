package de.uni.leipzig.tebaqa.tebaqacommons.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class PropertyUtils {

    private static final Logger LOGGER = LogManager.getLogger(PropertyUtils.class.getName());


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

}
