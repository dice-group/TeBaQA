package de.uni.leipzig.tebaqa.model;


import de.uni.leipzig.tebaqa.controller.QueryIsomorphism;

import java.util.HashSet;
import java.util.Set;

public class QueryTemplateMapping {
    private Set<String> askTemplates = new HashSet<>();
    private Set<String> selectTemplates = new HashSet<>();
    private Set<String> originalAskQueries = new HashSet<>();
    private Set<String> originalSelectQueries = new HashSet<>();

    private int numberOfClasses = 0;
    private int numberOfProperties = 0;

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

    public int getNumberOfClasses() {
        return numberOfClasses;
    }

    public int getNumberOfProperties() {
        return numberOfProperties;
    }
}
