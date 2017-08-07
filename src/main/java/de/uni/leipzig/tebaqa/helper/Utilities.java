package de.uni.leipzig.tebaqa.helper;

import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class Utilities {

    private static Logger log = Logger.getLogger(Utilities.class);

    /**
     * Use reflection to create a Spotlight instance with a given URL.
     * This is a workaround because the API from {@link Spotlight} is down.
     *
     * @param url The URL of the Spotlight instance.
     * @return A {@link Spotlight} instance with a custom URL.
     */
    public static Spotlight createCustomSpotlightInstance(String url) {
        Class<?> clazz = Spotlight.class;
        Spotlight spotlight;
        try {
            spotlight = (Spotlight) clazz.newInstance();
            Field requestURLField = spotlight.getClass().getDeclaredField("requestURL");
            requestURLField.setAccessible(true);
            requestURLField.set(spotlight, url);
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            spotlight = new Spotlight();
            log.error("Unable to change the Spotlight API URL using reflection. Using it's default value.", e);
        }
        return spotlight;
    }

    /**
     * Resolves all namespaces in a sparql query.
     *
     * @param query The query with namespaces
     * @return The given query where all namespaces are replaced with their full URI.
     */
    static String resolveNamespaces(String query) {
        Query q = QueryFactory.create(query);
        Map<String, String> nsPrefixMap = q.getPrefixMapping().getNsPrefixMap();
        q.setPrefixMapping(null);
        final String[] queryWithoutPrefix = {q.toString()};
        nsPrefixMap.forEach((s, s2) -> {
            queryWithoutPrefix[0] = queryWithoutPrefix[0].replace(s + ":", s2);
        });

        String[] split = queryWithoutPrefix[0].split(" ");
        for (int i = 0; i < split.length; i++) {
            if (split[0].startsWith("http://") || split[0].startsWith("https://")) {
                split[0] = "<" + split[0] + ">";
            }
        }
        return String.join(" ", split).trim();
    }

    /**
     * Converts a map to a JSON object and writes it to a file.
     *
     * @param outputPath The path of the file in which the content shall be written.
     * @param map        The map which shall be written to a file.
     */
    public static void writeToFile(String outputPath, Map<String, List<String>> map) {
        JSONObject json = new JSONObject();
        for (String entry : map.keySet()) {
            List<String> mappings = map.get(entry);
            json.put(entry, mappings);
        }

        try (FileWriter file = new FileWriter(outputPath)) {
            file.write(json.toString(2));
        } catch (IOException e) {
            log.error("Unable to write map to file", e);
        }
    }
}