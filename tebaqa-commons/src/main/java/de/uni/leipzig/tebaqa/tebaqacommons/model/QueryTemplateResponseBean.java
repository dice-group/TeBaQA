package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.List;

public class QueryTemplateResponseBean {

    private String question;
    private String lang;
    private List<String> templates;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getTemplates() {
        return templates;
    }

    public void setTemplates(List<String> templates) {
        this.templates = templates;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
