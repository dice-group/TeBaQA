package de.uni.leipzig.tebaqa.tebaqacommons.model;

public enum QueryType {

    QUERY_TYPE_UNKNOWN("UNK"),
    ASK_QUERY("ASK"),
    SELECT_QUERY("SEL"),
    SELECT_SUPERLATIVE_ASC_QUERY("SEL_ASC"),
    SELECT_SUPERLATIVE_DESC_QUERY("SEL_DEC"),
    SELECT_COUNT_QUERY("SEL_CNT");

    private final String code;

    QueryType(String code) {
        this.code = code;
    }
}
