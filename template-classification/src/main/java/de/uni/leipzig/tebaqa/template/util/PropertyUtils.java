package de.uni.leipzig.tebaqa.template.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class PropertyUtils {

    private static final Logger LOGGER = Logger.getLogger(PropertyUtils.class.getName());
    private static final Properties ALL_PROPERTIES = new Properties();


    static {
        ALL_PROPERTIES.putAll(getAllProperties("application.properties"));
        ALL_PROPERTIES.putAll(getAllProperties(Constants.TEMPLATE_CLASSIFICATION_PROP_FILE));
    }

    public PropertyUtils() {
        ALL_PROPERTIES.putAll(getAllProperties("application.properties"));
        ALL_PROPERTIES.putAll(getAllProperties(Constants.TEMPLATE_CLASSIFICATION_PROP_FILE));
    }

    private static Properties getAllProperties(String propertyFileName) {

        Properties prop = new Properties();
        InputStream input;
        try {
            input = PropertyUtils.class.getClassLoader().getResourceAsStream(propertyFileName);
            prop.load(input);
        } catch (IOException e) {
            LOGGER.error("Cannot load properties file " + propertyFileName);
            LOGGER.error(e.getMessage());
        }
        return prop;
    }


    private static String getFromProperties(String key, String defaultValue) {
        String value = ALL_PROPERTIES.getProperty(key);
        if (value == null) {
            value = defaultValue;
            ALL_PROPERTIES.put(key, value);
        }
        return value;
    }

    private static String getAbsolutePathFromClassPathOrPWD(String filename) {
        // First, check on the classpath
        URL resource = PropertyUtils.class.getClassLoader().getResource(filename);
        if (resource != null) {
            return resource.getPath();
        }

        // If file is not found on the classpath, then return current working directory
        return System.getProperty("user.dir") + System.getProperty("file.separator") + filename;
    }

    public static String getGraphsFileAbsolutePath(String datasetName) {
        return getAbsolutePathFromClassPathOrPWD(String.format(
                getFromProperties(Constants.SERIALIZED_GRAPHS_FILE, Constants.DEFAULT_SERIALIZED_GRAPHS_FILENAME),
                datasetName));
    }

    public static String getMappingsFileAbsolutePath(String datasetName) {
        return getAbsolutePathFromClassPathOrPWD(String.format(
                getFromProperties(Constants.SERIALIZED_MAPPINGS_FILE, Constants.DEFAULT_SERIALIZED_MAPPINGS_FILENAME),
                datasetName));
    }

    public static String getArffTrainFileAbsolutePath(String datasetName) {
        return getAbsolutePathFromClassPathOrPWD(String.format(
                getFromProperties(Constants.SERIALIZED_ARFF_TRAIN_FILE, Constants.DEFAULT_SERIALIZED_ARFF_TRAIN_FILENAME),
                datasetName));
    }

    public static String getClassifierFileAbsolutePath(String datasetName) {
        return getAbsolutePathFromClassPathOrPWD(String.format(
                getFromProperties(Constants.SERIALIZED_CLASSIFIER_FILE, Constants.DEFAULT_SERIALIZED_CLASSIFIER_FILENAME),
                datasetName));
    }
}
