package de.uni.leipzig.tebaqa.tebaqacommons.util;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class RestServiceConnector {

    private static final Logger LOGGER = Logger.getLogger(RestServiceConnector.class);

    public static <T> ResponseEntity<T> postParam(String serviceUrl, MultiValueMap<String, String> params, Class<T> clazz) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            return restTemplate.postForEntity(serviceUrl, request, clazz);
        } catch (RestClientException e) {
            LOGGER.error("Error connecting to " + serviceUrl + ": " + e.getMessage());
            throw e;
        }
    }

    public static <T> ResponseEntity<T> postJson(String serviceUrl, Object requestBody, Class<T> clazz) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(requestBody, headers);

        try {
            return restTemplate.postForEntity(serviceUrl, request, clazz);
        } catch (RestClientException e) {
            LOGGER.error("Error connecting to " + serviceUrl + ": " + e.getMessage());
            throw e;
        }
    }
}
