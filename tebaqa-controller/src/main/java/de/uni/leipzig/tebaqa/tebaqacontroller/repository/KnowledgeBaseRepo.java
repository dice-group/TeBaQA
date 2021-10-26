package de.uni.leipzig.tebaqa.tebaqacontroller.repository;


import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeBaseRepo extends JpaRepository<KnowledgeBase,Long> {



}
