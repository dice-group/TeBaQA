package de.uni.leipzig.tebaqa.tebaqacontroller.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SPARQLResultSet {
    public static int UNKNOWN_ANSWER_TYPE = -1;
    public static int BOOLEAN_ANSWER_TYPE = 0;
    public static int LIST_OF_RESOURCES_ANSWER_TYPE = 1;
    public static int SINGLE_ANSWER = 2;
    public static int NUMBER_ANSWER_TYPE = 3;
    public static int DATE_ANSWER_TYPE = 4;
    public static int MIXED_LIST_ANSWER_TYPE = 6;

    private int type;
    private List<String> resultSet;

    public SPARQLResultSet() {
        this.type = -1;
        this.resultSet = new ArrayList<>();
    }

    public SPARQLResultSet(List<String> resultSet) {
        this.type = -1;
        this.resultSet = resultSet.parallelStream().map(s -> s.contains("^^") ? s.split("\\^\\^")[0] : s).collect(Collectors.toList());
    }

    public SPARQLResultSet(List<String> resultSet, int type) {
        this.type = type;
        this.resultSet = resultSet.parallelStream().map(s -> s.contains("^^") ? s.split("\\^\\^")[0] : s).collect(Collectors.toList());
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<String> getResultSet() {
        return resultSet;
    }
}
