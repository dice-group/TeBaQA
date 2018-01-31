package de.uni.leipzig.tebaqa.helper;

public class TextUtilities {

    public static String trim(String s) {
        return s.trim().replaceAll("\n", " ").replaceAll("\\s{2,}", " ");
    }
}
