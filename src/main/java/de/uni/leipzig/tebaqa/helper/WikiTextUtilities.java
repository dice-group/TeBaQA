package de.uni.leipzig.tebaqa.helper;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

public class WikiTextUtilities {

    /**
     * Remove the pronunciation information that appears at the beginning of the article enclosed by ().
     * Additionally can be checked to contain unnecessary characters instead of blindly stripping based on brackets.
     * Source: https://github.com/dbpedia/chatbot/blob/master/src/main/java/chatbot/lib/api/SPARQL.java#L53
     *
     * @param text The text which may contain pronunciation information between brackets
     * @return The text without pronunciation information
     */
    public static String stripWikipediaContent(String text) {
        int indexStart = text.indexOf("(");
        int indexEnd;
        if (indexStart > 0) {
            indexEnd = text.indexOf(")", indexStart) + 2;
            if (indexEnd != -1) {
                return normalizeSpace(text.replace(text.substring(indexStart, indexEnd), ""));
            }
        } else if (indexStart == 0) {
            // When abstract starts with info on Disambiguation
            indexEnd = text.lastIndexOf("(disambiguation).)");
            if (indexEnd != -1) {
                return normalizeSpace(text.replace(text.substring(indexStart, indexEnd + 18), ""));
            }
        }
        return text;
    }
}
