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

    public static String getGraphsFileName() {
        String graphsFileName = ALL_PROPERTIES.getProperty(Constants.SERIALIZED_GRAPHS_FILE);
        if (graphsFileName == null) {
            graphsFileName = Constants.DEFAULT_SERIALIZED_GRAPHS_FILENAME;
            ALL_PROPERTIES.put(Constants.SERIALIZED_GRAPHS_FILE, graphsFileName);
        }
        return graphsFileName;
    }

    public static String getGraphsFileAbsolutePath() {
        String graphsFileName = getGraphsFileName();

        // First, check on the classpath
        URL resource = PropertyUtils.class.getClassLoader().getResource(graphsFileName);
        if (resource != null) {
            return resource.getPath();
        }

        // If file is not found on the classpath, then return current working directory
        return System.getProperty("user.dir") + System.getProperty("file.separator") + graphsFileName;
    }


    public static String getMappingsFileAbsolutePath() {
        String mappingsFileName = getMappingsFileName();

        // First, check on the classpath
        URL resource = PropertyUtils.class.getClassLoader().getResource(mappingsFileName);
        if (resource != null) {
            return resource.getPath();
        }

        // If file is not found on the classpath, then return current working directory
        return System.getProperty("user.dir") + System.getProperty("file.separator") + mappingsFileName;
    }

    private static String getMappingsFileName() {
        String mappingsFileName = ALL_PROPERTIES.getProperty(Constants.SERIALIZED_MAPPINGS_FILE);
        if (mappingsFileName == null) {
            mappingsFileName = Constants.DEFAULT_SERIALIZED_MAPPINGS_FILENAME;
            ALL_PROPERTIES.put(Constants.SERIALIZED_MAPPINGS_FILE, mappingsFileName);
        }
        return mappingsFileName;
    }

}
