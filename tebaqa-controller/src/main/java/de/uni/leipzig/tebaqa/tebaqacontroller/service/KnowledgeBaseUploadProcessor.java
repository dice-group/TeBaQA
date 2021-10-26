package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch.TeBaQAIndexer;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ESConnectionProperties;
import de.uni.leipzig.tebaqa.tebaqacommons.util.PropertyUtils;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.FileUploadStatus;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KBUploadStatus;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KnowledgeBase;
import de.uni.leipzig.tebaqa.tebaqacontroller.validation.ValidationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class KnowledgeBaseUploadProcessor implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(KnowledgeBaseUploadProcessor.class);
    private Long kbId;
    private Set<String> excludedPredicates;
    private KnowledgeBaseService knowledgeBaseService;
    private UploadStatusService uploadStatusService;

    private KnowledgeBaseUploadProcessor() {
    }

    public KnowledgeBaseUploadProcessor(long kbId, KnowledgeBaseService knowledgeBaseService, UploadStatusService uploadStatusService) {
        this(kbId, knowledgeBaseService, uploadStatusService, Collections.emptySet());
    }

    public KnowledgeBaseUploadProcessor(long kbId, KnowledgeBaseService knowledgeBaseService, UploadStatusService uploadStatusService, Set<String> excludedPredicates) {
        this.kbId = kbId;
        this.knowledgeBaseService = knowledgeBaseService;
        this.uploadStatusService = uploadStatusService;
        this.excludedPredicates = excludedPredicates;
    }

    @Override
    public void run() {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(kbId);

        Properties allProperties = PropertyUtils.getAllProperties("indexing.properties");
        ESConnectionProperties esProps = new ESConnectionProperties(
                allProperties.getProperty("target.host.scheme"),
                allProperties.getProperty("target.host.name"),
                allProperties.getProperty("target.host.port"),
                PropertyUtils.getESEntityIndexName(knowledgeBase.getName()),
                PropertyUtils.getESClassIndexName(knowledgeBase.getName()),
                PropertyUtils.getESPropertyIndexName(knowledgeBase.getName()),
                null
        );
        TeBaQAIndexer indexer = new TeBaQAIndexer(esProps);

        // KB upload status - Started
        KBUploadStatus kbUploadStatus = new KBUploadStatus(kbId, false);
        uploadStatusService.saveKBUploadStatus(kbUploadStatus);

        // Data files upload
        List<String> dataInputs = knowledgeBase.getDataFiles();
        for(String input : dataInputs) {
            FileUploadStatus fileUploadStatus = new FileUploadStatus(this.kbId, input);
            try {
                uploadStatusService.saveFileUploadStatus(fileUploadStatus);
                if(knowledgeBase.isUploadedViaLink()) {
                    indexer.indexDataURLsOrThrowException(Collections.singletonList(input), excludedPredicates);
                } else {
                    indexer.indexDataFilesOrThrowException(Collections.singletonList(input), excludedPredicates);
                }
                fileUploadStatus.setStatus(1);
                uploadStatusService.saveFileUploadStatus(fileUploadStatus);

            } catch (Exception e) {
                LOGGER.error("Failed to index: " + input);
                LOGGER.error(e.getMessage());

                fileUploadStatus.setStatus(2);
                uploadStatusService.saveFileUploadStatus(fileUploadStatus);
            }
        }

        // Ontology files upload
        List<String> ontologyInputs;
        if(knowledgeBase.isUploadedViaLink()) {
            ontologyInputs = knowledgeBase.getOntologyFiles();
        } else {
            // TODO files
            ontologyInputs = knowledgeBase.getOntologyFiles();
        }
        for(String input : ontologyInputs) {
            FileUploadStatus fileUploadStatus = new FileUploadStatus(this.kbId, input);
            try {
                uploadStatusService.saveFileUploadStatus(fileUploadStatus);
                if(knowledgeBase.isUploadedViaLink()) {
                    indexer.indexOntologyURLs(Collections.singletonList(input));
                } else {
                    indexer.indexOntologyFiles(Collections.singletonList(input));
                }
                fileUploadStatus.setStatus(1);
                uploadStatusService.saveFileUploadStatus(fileUploadStatus);

            } catch (Exception e) {
                LOGGER.error("Failed to index: " + input);
                LOGGER.error(e.getMessage());

                fileUploadStatus.setStatus(2);
                uploadStatusService.saveFileUploadStatus(fileUploadStatus);
            }
        }

        kbUploadStatus.setStatus(true);
        uploadStatusService.saveKBUploadStatus(kbUploadStatus);
    }
}


