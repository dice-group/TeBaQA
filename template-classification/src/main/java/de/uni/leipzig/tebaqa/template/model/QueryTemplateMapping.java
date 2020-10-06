package de.uni.leipzig.tebaqa.template.model;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QueryTemplateMapping {
    private final Set<String> askTemplates = new HashSet<>();
    private final Set<String> selectTemplates = new HashSet<>();
    private final Set<String> originalAskQueries = new HashSet<>();
    private final Set<String> originalSelectQueries = new HashSet<>();

    private final Set<String> selectSuperlativeAscTemplate = new HashSet<>();
    private final Set<String> selectSuperlativeDescTemplate = new HashSet<>();
    private final Set<String> originalSuperlativeAscQueries = new HashSet<>();
    private final Set<String> originalSuperlativeDescQueries = new HashSet<>();

    private final Set<String> originalCountQueries = new HashSet<>();
    private final Set<String> selectCountTemplates = new HashSet<>();


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

    public void addSelectTemplate(String template, String originalQuery) {

        if (this.getSelectTemplates().isEmpty()) {
            this.selectTemplates.add(template);
            this.originalSelectQueries.add(originalQuery);
        }
    }

    public void addSelectSuperlativeAscTemplate(String template, String originalQuery) {
        if (this.getSelectSuperlativeAscTemplate().isEmpty()) {
            this.selectSuperlativeAscTemplate.add(template);
            this.originalSuperlativeAscQueries.add(originalQuery);
        }
    }

    public void addCountTemplate(String template, String originalQuery) {

        if (this.getSelectCountTemplates().isEmpty()) {
            this.selectCountTemplates.add(template);
            this.originalCountQueries.add(originalQuery);
        }
    }

    public void addSelectSuperlativeDescTemplate(String template, String originalQuery) {

        if (this.selectSuperlativeDescTemplate.isEmpty()) {
            this.selectSuperlativeDescTemplate.add(template);
            this.originalSuperlativeDescQueries.add(originalQuery);
        }
    }

    public void addAskTemplate(String template, String originalQuery) {

        if (this.askTemplates.isEmpty()) {
            this.askTemplates.add(template);
            this.originalAskQueries.add(originalQuery);
        }
    }
    /*public void addSelectTemplate(String template, String originalQuery) {
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
    }*/

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

    public List<Integer> getNumberOfClasses() {
        return numberOfClasses;
    }

    public List<Integer> getNumberOfProperties() {
        return numberOfProperties;
    }

    public Set<String> getAllAvailableTemples() {
        Set<String> allTemplates = new HashSet<>();
        allTemplates.addAll(this.getAskTemplates());
        allTemplates.addAll(this.getSelectTemplates());
        allTemplates.addAll(this.getSelectCountTemplates());
        allTemplates.addAll(this.getSelectSuperlativeAscTemplate());
        allTemplates.addAll(this.getSelectSuperlativeDescTemplate());
        return allTemplates;
    }
}
