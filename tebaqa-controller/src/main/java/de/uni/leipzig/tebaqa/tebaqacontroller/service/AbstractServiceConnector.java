package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class AbstractServiceConnector {

    private static final Logger LOGGER = Logger.getLogger(AbstractServiceConnector.class);

    public <T> ResponseEntity<T> connect(String serviceUrl, MultiValueMap<String, String> params, Class<T> clazz) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            return restTemplate.postForEntity(serviceUrl, request, clazz);
        } catch (RestClientException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }
}
