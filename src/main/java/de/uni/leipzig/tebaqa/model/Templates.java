package de.uni.leipzig.tebaqa.model;

public class Templates {
    private final String askPrefix="ASK {";
    private final String selectPrefix="SELECT DISTINCT ?placeholder WHERE{";
    private final String selectCountPrefix="SELECT (COUNT(DISTINCT ?placeholder) AS ?count) WHERE {";
    private final String superlativeDescPrefix="SELECT DISTINCT ?placeholder WHERE {";
    private final String superlativeAscPrefix="SELECT DISTINCT ?placeholder WHERE {";

    private final String askSuffix="}";
    private final String selectSuffix="}";
    private final String selectCountSuffix="}";
    private final String superlativeDescSuffix="ORDER BY DESC(?area) OFFSET 0 LIMIT 1";
    private final String superlativeAscSuffix="ORDER BY ASC(?area) OFFSET 0 LIMIT 1";

    public String getAskPrefix() {
        return askPrefix;
    }

    public String getSelectPrefix() {
        return selectPrefix;
    }

    public String getSelectCountPrefix() {
        return selectCountPrefix;
    }

    public String getSuperlativeDescPrefix() {
        return superlativeDescPrefix;
    }

    public String getSuperlativeAscPrefix() {
        return superlativeAscPrefix;
    }

    public String getAskSuffix() {
        return askSuffix;
    }

    public String getSelectSuffix() {
        return selectSuffix;
    }

    public String getSelectCountSuffix() {
        return selectCountSuffix;
    }

    public String getSuperlativeDescSuffix() {
        return superlativeDescSuffix;
    }

    public String getSuperlativeAscSuffix() {
        return superlativeAscSuffix;
    }
}
