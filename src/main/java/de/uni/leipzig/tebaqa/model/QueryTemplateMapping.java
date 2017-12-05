package de.uni.leipzig.tebaqa.model;


import de.uni.leipzig.tebaqa.controller.QueryIsomorphism;

import java.util.HashSet;
import java.util.Set;

public class QueryTemplateMapping {
    private Set<String> askTemplates = new HashSet<>();
    private Set<String> selectTemplates = new HashSet<>();
    private Set<String> originalAskQueries = new HashSet<>();
    private Set<String> originalSelectQueries = new HashSet<>();

    private Set<String> selectSuperlativeAscTemplate = new HashSet<>();
    private Set<String> selectSuperlativeDescTemplate = new HashSet<>();
    private Set<String> originalSuperlativeAscQueries = new HashSet<>();
    private Set<String> originalSuperlativeDescQueries = new HashSet<>();

    private Set<String> originalCountQueries = new HashSet<>();
    private Set<String> selectCountTemplates = new HashSet<>();


    private int numberOfClasses;
    private int numberOfProperties;

    public QueryTemplateMapping(int numberOfClasses, int numberOfProperties) {
        this.numberOfClasses = numberOfClasses;
        this.numberOfProperties = numberOfProperties;
    }

    public void addSelectTemplate(String template, String originalQuery) {
        final boolean[] templateIsIsomorph = {false};
        originalSelectQueries.forEach(s -> {
            if (QueryIsomorphism.areIsomorph(s, originalQuery)) {
                templateIsIsomorph[0] = true;
            }
        });
        if (!templateIsIsomorph[0]) {
            this.selectTemplates.add(template);
            this.originalSelectQueries.add(originalQuery);
        }
    }

    public void addSelectSuperlativeAscTemplate(String template, String originalQuery) {
        final boolean[] templateIsIsomorph = {false};
        originalSuperlativeAscQueries.forEach(s -> {
            if (QueryIsomorphism.areIsomorph(s, originalQuery)) {
                templateIsIsomorph[0] = true;
            }
        });
        if (!templateIsIsomorph[0]) {
            this.selectSuperlativeAscTemplate.add(template);
            this.originalSuperlativeAscQueries.add(originalQuery);
        }
    }

    public void addCountTemplate(String template, String originalQuery) {
        final boolean[] templateIsIsomorph = {false};
        originalCountQueries.forEach(s -> {
            if (QueryIsomorphism.areIsomorph(s, originalQuery)) {
                templateIsIsomorph[0] = true;
            }
        });
        if (!templateIsIsomorph[0]) {
            this.selectCountTemplates.add(template);
            this.originalCountQueries.add(originalQuery);
        }
    }

    public void addSelectSuperlativeDescTemplate(String template, String originalQuery) {
        final boolean[] templateIsIsomorph = {false};
        originalSuperlativeDescQueries.forEach(s -> {
            if (QueryIsomorphism.areIsomorph(s, originalQuery)) {
                templateIsIsomorph[0] = true;
            }
        });
        if (!templateIsIsomorph[0]) {
            this.selectSuperlativeDescTemplate.add(template);
            this.originalSuperlativeDescQueries.add(originalQuery);
        }
    }

    public void addAskTemplate(String template, String originalQuery) {
        final boolean[] templateIsIsomorph = {false};
        originalAskQueries.forEach(s -> {
            if (QueryIsomorphism.areIsomorph(s, originalQuery)) {
                templateIsIsomorph[0] = true;
            }
        });
        if (!templateIsIsomorph[0]) {
            this.askTemplates.add(template);
            this.originalAskQueries.add(originalQuery);
        }
    }

    public Set<String> getAskTemplates() {
        return askTemplates;
    }

    public Set<String> getSelectTemplates() {
        return selectTemplates;
    }

    public Set<String> getSelectSuperlativeAscTemplate() {
        return selectSuperlativeAscTemplate;
    }

    public Set<String> getSelectSuperlativeDescTemplate() {
        return selectSuperlativeDescTemplate;
    }

    public Set<String> getSelectCountTemplates() {
        return selectCountTemplates;
    }

    public int getNumberOfClasses() {
        return numberOfClasses;
    }

    public int getNumberOfProperties() {
        return numberOfProperties;
    }
}
