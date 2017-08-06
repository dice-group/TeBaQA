package de.uni.leipzig.tebaqa.helper;

import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
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
     * @param query
     * @return
     */
    public static String resolveNamespaces(String query) {
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
}