package de.uni.leipzig.tebaqa.tebaqacommons.model;

public enum QuestionAnswerType {
    UNKNOWN_ANSWER_TYPE("UNKNOWN_ANSWER_TYPE"),
    //    = -1;
    BOOLEAN_ANSWER_TYPE("BOOLEAN_ANSWER_TYPE"),
    //    = 0;
    LIST_OF_RESOURCES_ANSWER_TYPE("LIST_OF_RESOURCES_ANSWER_TYPE"),
    //    = 1;
    SINGLE_ANSWER("SINGLE_ANSWER"),
    //    = 2;
    NUMBER_ANSWER_TYPE("NUMBER_ANSWER_TYPE"),
    //    = 3;
    DATE_ANSWER_TYPE("DATE_ANSWER_TYPE"),
    //    = 4;
    MIXED_LIST_ANSWER_TYPE("MIXED_LIST_ANSWER_TYPE");
//    = 6;

    private final String code;

    QuestionAnswerType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static QuestionAnswerType forCode(String code) {
        for (QuestionAnswerType qt : QuestionAnswerType.values()) {
            if (qt.getCode().equalsIgnoreCase(code))
                return qt;
        }
        return null;
    }

}
