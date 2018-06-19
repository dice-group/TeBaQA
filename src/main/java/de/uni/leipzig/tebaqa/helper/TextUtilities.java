package de.uni.leipzig.tebaqa.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextUtilities {

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
}
