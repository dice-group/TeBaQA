package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import de.uni.leipzig.tebaqa.tebaqacommons.model.RestServiceConfiguration;
import de.uni.leipzig.tebaqa.tebaqacommons.util.RestServiceConnector;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KnowledgeBaseForm;
import de.uni.leipzig.tebaqa.tebaqacontroller.utils.ControllerPropertyUtils;
import de.uni.leipzig.tebaqa.tebaqacontroller.validation.ValidationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@Service
public class TripleStoreService {

    private static final Logger LOGGER = LogManager.getLogger(TripleStoreService.class);

    private final String TRIPLE_STORE_URL;
    private final static String CREATE_DATASET_ENDPOINT = "$/datasets";
    private final static String UPLOAD_DATA_ENDPOINT = "%s/data";

    public TripleStoreService() {
        TRIPLE_STORE_URL = ControllerPropertyUtils.getTripleStoreUrl();
    }

    public boolean createDataset(String dsName) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("dbName", dsName);
        params.add("dbType", "tdb2");

        try {
            RestServiceConnector.postParam(TRIPLE_STORE_URL + CREATE_DATASET_ENDPOINT, params, String.class);
            LOGGER.info("Created dataset /" + dsName);
            return true;
        } catch (RestClientException e) {
            return false;
        }

    }

    public Map<String, Integer> uploadFile(String dsName, String path, KnowledgeBaseForm.UploadType uploadType) {
        try {
            MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();

            Resource resource;
            if(KnowledgeBaseForm.UploadType.FILE.equals(uploadType)) {
                resource = new FileSystemResource(path);
            } else {
                resource = new UrlResource(path) {
                    @Override
                    public String getFilename() {
                        return ValidationUtils.getFileName(path);
                    }
                };
            }
            bodyMap.add("file", resource);
            RequestEntity<MultiValueMap<String, Object>> request =
                    RequestEntity.post(URI.create(TRIPLE_STORE_URL + String.format(UPLOAD_DATA_ENDPOINT, dsName)))
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(bodyMap);

            RestTemplate restTemplate = new RestTemplate();
            HttpComponentsClientHttpRequestFactory requestFactory = new
                    HttpComponentsClientHttpRequestFactory();
            requestFactory.setBufferRequestBody(false);
            restTemplate.setRequestFactory(requestFactory);
            Map<String, Integer> response = (Map<String, Integer>)restTemplate.exchange(request, Map.class).getBody();
            LOGGER.info("Uploaded to triple store " + path);
            LOGGER.info(response);
            return response;
        } catch (Exception e) {
            LOGGER.error("Could not upload " + path);
            return null;
        }
    }

    public static void testCreate() {
        TripleStoreService o = new TripleStoreService();
        o.createDataset("sample");
    }

    public static void main(String[] args) {
        TripleStoreService o = new TripleStoreService();
        o.uploadFile("sample", "C:\\Users\\Hardik\\Downloads\\persondata_en.ttl", KnowledgeBaseForm.UploadType.FILE);
    }

}
