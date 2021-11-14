package de.uni.leipzig.tebaqa.tebaqacontroller.service;

import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.FileUploadStatus;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KBUploadStatus;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KnowledgeBase;
import de.uni.leipzig.tebaqa.tebaqacontroller.repository.FileUploadStatusRepo;
import de.uni.leipzig.tebaqa.tebaqacontroller.repository.KBUploadStatusRepo;
import de.uni.leipzig.tebaqa.tebaqacontroller.repository.KnowledgeBaseRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class KnowledgeBaseService {

    @Autowired
    KnowledgeBaseRepo knowledgeBaseRepo;

    @Autowired
    KBUploadStatusRepo uploadStatusRepo;

    @Autowired
    FileUploadStatusRepo fileUploadStatusRepo;

    @Autowired
    UploadStatusService uploadStatusService;

    @Autowired
    TripleStoreService tripleStoreService;

    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        knowledgeBaseRepo.saveAndFlush(knowledgeBase);
        return knowledgeBase;
    }

    public KnowledgeBase saveAndIndex(KnowledgeBase knowledgeBase) {
        knowledgeBaseRepo.saveAndFlush(knowledgeBase);

        KnowledgeBaseUploadProcessor dataUploadTask = new KnowledgeBaseUploadProcessor(knowledgeBase.getId(), this, uploadStatusService, tripleStoreService);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(dataUploadTask);

        return knowledgeBase;
    }

    public KnowledgeBase getById(long id) {
        Optional<KnowledgeBase> knowledgeBaseResponse = knowledgeBaseRepo.findById(id);

        KnowledgeBase knowledgeBase = null;
        if (knowledgeBaseResponse.isPresent()) {
            knowledgeBase = knowledgeBaseResponse.get();
            populateUploadStatusIn(knowledgeBase);
        }
        return knowledgeBase;
    }

    public List<KnowledgeBase> getAllKnowledgeBases() {
        List<KnowledgeBase> all = knowledgeBaseRepo.findAll();
        all.forEach(this::populateUploadStatusIn);
        return all;
    }

    private void populateUploadStatusIn(KnowledgeBase knowledgeBase) {

        // Overall completion status
        KBUploadStatus kbUploadStatus = uploadStatusRepo.findByKnowledgeBaseId(knowledgeBase.getId());
        if (kbUploadStatus != null && kbUploadStatus.isStatus())
            knowledgeBase.setKbUploadStatus(kbUploadStatus.isStatus());

        // Completion status of each file/link
        List<FileUploadStatus> uploads = fileUploadStatusRepo.findByKnowledgeBaseId(knowledgeBase.getId());
        knowledgeBase.setFileUploadStatus(uploads);

    }
}
