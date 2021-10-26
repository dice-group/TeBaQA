package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Set;

public class QueryRankingRequestBody {
    private String kbName;
    private String question;
    private String lang;
    private Set<String> queryTemplates;
    private EntityLinkingResponseBean linkedResourcesJson;

    public QueryRankingRequestBody() {}

    public QueryRankingRequestBody(String question, String lang, Set<String> queryTemplates, EntityLinkingResponseBean linkedResourcesJson, String kbName) {
        this.kbName = kbName;
        this.question = question;
        this.lang = lang;
        this.queryTemplates = queryTemplates;
        this.linkedResourcesJson = linkedResourcesJson;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Set<String> getQueryTemplates() {
        return queryTemplates;
    }

    public void setQueryTemplates(Set<String> queryTemplates) {
        this.queryTemplates = queryTemplates;
    }

    public EntityLinkingResponseBean getLinkedResourcesJson() {
        return linkedResourcesJson;
    }

    public void setLinkedResourcesJson(EntityLinkingResponseBean linkedResourcesJson) {
        this.linkedResourcesJson = linkedResourcesJson;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }
}
