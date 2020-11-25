package de.uni.leipzig.tebaqa.tebaqacommons.model;

public class ESConnectionProperties extends RestServiceConfiguration {
    private String entityIndex;
    private String classIndex;
    private String propertyIndex;
    private String literalIndex;

    public ESConnectionProperties(String scheme, String hostname, String port, String entityIndex, String classIndex, String propertyIndex, String literalIndex) {
        this.scheme = scheme;
        this.hostname = hostname;
        this.port = port;
        this.entityIndex = entityIndex;
        this.classIndex = classIndex;
        this.propertyIndex = propertyIndex;
        this.literalIndex = literalIndex;
    }

    public String getEntityIndex() {
        return entityIndex;
    }

    public void setEntityIndex(String entityIndex) {
        this.entityIndex = entityIndex;
    }

    public String getClassIndex() {
        return classIndex;
    }

    public void setClassIndex(String classIndex) {
        this.classIndex = classIndex;
    }

    public String getPropertyIndex() {
        return propertyIndex;
    }

    public void setPropertyIndex(String propertyIndex) {
        this.propertyIndex = propertyIndex;
    }

    public String getLiteralIndex() {
        return literalIndex;
    }

    public void setLiteralIndex(String literalIndex) {
        this.literalIndex = literalIndex;
    }
}
