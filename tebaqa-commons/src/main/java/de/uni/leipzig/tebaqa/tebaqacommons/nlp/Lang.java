package de.uni.leipzig.tebaqa.tebaqacommons.nlp;

import java.io.IOException;

public enum Lang {

    EN("en"),
    DE("de");

    private final String languageCode;

    Lang(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public static Lang getForCode(String languageCode) {
        for (Lang val : Lang.values()) {
            if (val.languageCode.equalsIgnoreCase(languageCode))
                return val;
        }
        return null;
    }

    public SemanticAnalysisHelper getSemanticAnalysisHelper() throws IOException {
        return new SemanticAnalysisHelper(this);
    }
}