package de.uni.leipzig.tebaqa.template.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import de.uni.leipzig.tebaqa.tebaqacommons.model.QueryType;

import java.util.*;

public class QueryTemplateMapping {
    private final Set<String> askQueries = new HashSet<>();
    private final Set<String> selectQueries = new HashSet<>();
    private final Set<String> superlativeAscQueries = new HashSet<>();
    private final Set<String> superlativeDescQueries = new HashSet<>();
    private final Set<String> countQueries = new HashSet<>();
    private String askTemplate;
    private String selectTemplate;
    private String superlativeAscTemplate;
    private String superlativeDescTemplate;
    private String countTemplates;

    private final List<Integer> numberOfClasses;
    private final List<Integer> numberOfProperties;

    public QueryTemplateMapping(int numberOfClasses, int numberOfProperties) {
        this.numberOfClasses = new ArrayList<>();
        this.numberOfClasses.add(numberOfClasses);
        this.numberOfProperties = new ArrayList<>();
        this.numberOfProperties.add(numberOfProperties);
    }

    public QueryTemplateMapping() {
        this.numberOfClasses = new ArrayList<>();
        this.numberOfProperties = new ArrayList<>();
    }

    public String getAskTemplate() {
        return askTemplate;
    }

    public void setAskTemplate(String askTemplate) {
        this.askTemplate = askTemplate;
    }

    public Set<String> getAskQueries() {
        return askQueries;
    }

    public void setAskQueries(Set<String> askQueries) {
        this.askQueries.addAll(askQueries);
    }

    public String getSelectTemplate() {
        return selectTemplate;
    }

    public void setSelectTemplate(String selectTemplate) {
        this.selectTemplate = selectTemplate;
    }

    public Set<String> getSelectQueries() {
        return selectQueries;
    }

    public void setSelectQueries(Set<String> selectQueries) {
        this.selectQueries.addAll(selectQueries);
    }

    public String getSuperlativeAscTemplate() {
        return superlativeAscTemplate;
    }

    public void setSuperlativeAscTemplate(String superlativeAscTemplate) {
        this.superlativeAscTemplate = superlativeAscTemplate;
    }

    public Set<String> getSuperlativeAscQueries() {
        return superlativeAscQueries;
    }

    public void setSuperlativeAscQueries(Set<String> superlativeAscQueries) {
        this.superlativeAscQueries.addAll(superlativeAscQueries);
    }

    public String getSuperlativeDescTemplate() {
        return superlativeDescTemplate;
    }

    public void setSuperlativeDescTemplate(String superlativeDescTemplate) {
        this.superlativeDescTemplate = superlativeDescTemplate;
    }

    public Set<String> getSuperlativeDescQueries() {
        return superlativeDescQueries;
    }

    public void setSuperlativeDescQueries(Set<String> superlativeDescQueries) {
        this.superlativeDescQueries.addAll(superlativeDescQueries);
    }

    public String getCountTemplates() {
        return countTemplates;
    }

    public void setCountTemplates(String countTemplates) {
        this.countTemplates = countTemplates;
    }

    public Set<String> getCountQueries() {
        return countQueries;
    }

    public void setCountQueries(Set<String> countQueries) {
        this.countQueries.addAll(countQueries);
    }

    public void setSelectTemplate(String template, String originalQuery) {
        if (this.selectTemplate == null)
            this.selectTemplate = template;

//        this.selectQueries.add(originalQuery);
    }

    public void setSelectSuperlativeAscTemplate(String template, String originalQuery) {
        if (this.superlativeAscTemplate == null)
            this.superlativeAscTemplate = template;

//        this.superlativeAscQueries.add(originalQuery);
    }

    public void setCountTemplate(String template, String originalQuery) {
        if (this.countTemplates == null)
            this.countTemplates = template;

        this.countQueries.add(originalQuery);
    }

    public void setSelectSuperlativeDescTemplate(String template, String originalQuery) {
        if (this.superlativeDescTemplate == null)
            this.superlativeDescTemplate = template;

//        this.superlativeDescQueries.add(originalQuery);
    }

    public void setAskTemplate(String template, String originalQuery) {
        if (this.askTemplate == null)
            this.askTemplate = template;

//        this.askQueries.add(originalQuery);
    }

    public List<Integer> getNumberOfClasses() {
        return numberOfClasses;
    }

    public List<Integer> getNumberOfProperties() {
        return numberOfProperties;
    }

    @JsonIgnore
    public Set<String> getAllAvailableTemplates() {
        Set<String> allTemplates = new HashSet<>();
        if (askTemplate != null) allTemplates.add(askTemplate);
        if (selectTemplate != null) allTemplates.add(selectTemplate);
        if (superlativeAscTemplate != null) allTemplates.add(superlativeAscTemplate);
        if (superlativeDescTemplate != null) allTemplates.add(superlativeDescTemplate);
        if (countTemplates != null) allTemplates.add(countTemplates);
        return allTemplates;
    }

    public Set<String> getTemplatesFor(QueryType queryType) {
        if (QueryType.ASK_QUERY.equals(queryType)) return getSingletonSetIfNotNull(askTemplate);
        else if (QueryType.SELECT_QUERY.equals(queryType)) return getSingletonSetIfNotNull(selectTemplate);
        else if (QueryType.SELECT_SUPERLATIVE_ASC_QUERY.equals(queryType))
            return getSingletonSetIfNotNull(superlativeAscTemplate);
        else if (QueryType.SELECT_SUPERLATIVE_DESC_QUERY.equals(queryType))
            return getSingletonSetIfNotNull(superlativeDescTemplate);
        else if (QueryType.SELECT_COUNT_QUERY.equals(queryType)) return getSingletonSetIfNotNull(countTemplates);
        else return getAllAvailableTemplates();
//        else return Collections.emptySet();
    }
    
    private Set<String> getSingletonSetIfNotNull(String template) {
        return template == null ? new HashSet<>(0) : Collections.singleton(template);
    }
}
