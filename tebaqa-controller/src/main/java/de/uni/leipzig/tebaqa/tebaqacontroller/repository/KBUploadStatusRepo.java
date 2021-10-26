package de.uni.leipzig.tebaqa.tebaqacontroller.repository;


import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KBUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KBUploadStatusRepo extends JpaRepository<KBUploadStatus,Long> {

    @Query("select e from KBUploadStatus e where e.kbId = :knowledgeBaseId")
    KBUploadStatus findByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

}
