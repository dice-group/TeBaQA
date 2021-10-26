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

@Service
public class UploadStatusService {

    @Autowired
    KBUploadStatusRepo uploadStatusRepo;

    @Autowired
    FileUploadStatusRepo fileUploadStatusRepo;

    public KBUploadStatus saveKBUploadStatus(KBUploadStatus uploadStatus) {
        uploadStatusRepo.saveAndFlush(uploadStatus);
        return uploadStatus;
    }

    public FileUploadStatus saveFileUploadStatus(FileUploadStatus uploadStatus) {
        fileUploadStatusRepo.saveAndFlush(uploadStatus);
        return uploadStatus;
    }

//    public KnowledgeBase getById(long id) {
//        Optional<KnowledgeBase> knowledgeBaseResponse = knowledgeBaseRepo.findById(id);
//
//        KnowledgeBase knowledgeBase = null;
//        if(knowledgeBaseResponse.isPresent()) {
//            knowledgeBase = knowledgeBaseResponse.get();
//            populateUploadStatusIn(knowledgeBase);
//        }
//        return knowledgeBase;
//    }
//

}
