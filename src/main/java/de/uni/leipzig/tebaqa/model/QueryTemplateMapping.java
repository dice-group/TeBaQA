package de.uni.leipzig.tebaqa.model;

import java.util.HashSet;
import java.util.Set;

public class QueryTemplateMapping {
    private Set<String> askTemplates = new HashSet<>();
    private Set<String> selectTemplates = new HashSet<>();

    private int numberOfClasses = 0;
    private int numberOfProperties = 0;

    public QueryTemplateMapping(int numberOfClasses, int numberOfProperties) {
        this.numberOfClasses = numberOfClasses;
        this.numberOfProperties = numberOfProperties;
    }

    public void addSelectTemplate(String template) {
        this.selectTemplates.add(template);
    }

    public void addAskTemplate(String template) {
        this.askTemplates.add(template);
    }

    public Set<String> getAskTemplates() {
        return askTemplates;
    }

    public void setAskTemplates(Set<String> askTemplates) {
        this.askTemplates = askTemplates;
    }

    public Set<String> getSelectTemplates() {
        return selectTemplates;
    }

    public void setSelectTemplates(Set<String> selectTemplates) {
        this.selectTemplates = selectTemplates;
    }

    public int getNumberOfClasses() {
        return numberOfClasses;
    }

    public int getNumberOfProperties() {
        return numberOfProperties;
    }
}
