package de.uni.leipzig.tebaqa.template.util;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO methods are in common, change references
public class TextUtils {

    public final static String NON_WORD_CHARACTERS_REGEX = "[^a-zA-Z0-9_äÄöÖüÜ']";

    public static String trim(String s) {
        return s.trim().replaceAll("\n", " ").replaceAll("\\s{2,}", " ").trim();
    }

    public static int countWords(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        List<String> words = new ArrayList<>(Arrays.asList(s.split(NON_WORD_CHARACTERS_REGEX)));
        words.removeIf(String::isEmpty);
        return words.size();
    }

    public static String cleanQuery(String queryString) {
        Query query = QueryFactory.create(queryString);
        query.setPrefixMapping(null);
        return query.toString().trim()
                //replace newlines with space
                .replaceAll("\n", " ")
                //replace every variable with ?
                .replaceAll("\\?[a-zA-Z\\d]+", " ? ")
                //replace every number(e.g. 2 or 2.5) with a ?
                .replaceAll("\\s+\\d+\\.?\\d*", " ? ")
                //replace everything in quotes with ?
                .replaceAll("([\"'])(?:(?=(\\\\?))\\2.)*?\\1", " ? ")
                //remove everything between <>
                .replaceAll("<\\S*>", " <> ")
                //remove all SELECT, ASK, DISTINCT and WHERE keywords
                .replaceAll("(?i)(select|ask|where|distinct)", " ")
                //remove every consecutive spaces
                .replaceAll("\\s+", " ");
    }
}
