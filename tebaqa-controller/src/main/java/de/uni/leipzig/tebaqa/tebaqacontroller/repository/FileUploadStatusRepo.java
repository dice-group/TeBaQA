package de.uni.leipzig.tebaqa.tebaqacontroller.repository;


import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.FileUploadStatus;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KBUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileUploadStatusRepo extends JpaRepository<FileUploadStatus,Long> {

    @Query("select e from FileUploadStatus e where e.kbId = :knowledgeBaseId")
    List<FileUploadStatus> findByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

}
