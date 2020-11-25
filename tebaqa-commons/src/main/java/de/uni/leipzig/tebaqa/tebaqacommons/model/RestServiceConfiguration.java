package de.uni.leipzig.tebaqa.tebaqacommons.model;

public class RestServiceConfiguration {

    public static final String URL_TEMPLATE = "%s://%s:%s/";

    protected String scheme;
    protected String hostname;
    protected String port;

    public RestServiceConfiguration() {
    }

    public RestServiceConfiguration(String scheme, String hostname, String port) {
        this.scheme = scheme;
        this.hostname = hostname;
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUrl() {
        return String.format(URL_TEMPLATE, this.scheme, this.hostname, this.port);
    }
}
