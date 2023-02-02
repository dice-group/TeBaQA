package de.uni.leipzig.tebaqa.queryranking.elasticsearch;

import de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch.SearchService;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ESConnectionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static de.uni.leipzig.tebaqa.queryranking.QueryRankingApplication.LOGGER;

@Component
public class SearchProvider {

    private static SearchService searchService = null;

    private static String FILE_NAME;

    @Value("${spring.application.query-ranking.properties}")
    public void setFileName(String name){
        this.FILE_NAME = name;
    }

    public static SearchService getSingletonSearchClient() {
        if (searchService == null) {
            Properties prop = new Properties();
            InputStream input;
            try {

                input = SearchService.class.getResourceAsStream(FILE_NAME);
                prop.load(input);

                String hostname = prop.getProperty("el_hostname");
                String port = prop.getProperty("el_port");
                String scheme = prop.getProperty("scheme");
                String resourceIndex = prop.getProperty("resource_index");
                String propertyIndex = prop.getProperty("property_index");
                String classIndex = prop.getProperty("class_index");
                String literalIndex = prop.getProperty("literal_index");

                ESConnectionProperties properties = new ESConnectionProperties(scheme, hostname, port, resourceIndex, classIndex, propertyIndex, literalIndex);
                searchService = new SearchService(properties);

            } catch (IOException e) {
                LOGGER.error("Cannot read entity linking properties file: " + e.getMessage());
            }
        }
        return searchService;
    }

}
