package de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload;


import com.google.common.collect.Lists;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "KNOWLEDGE_BASE")
public class KnowledgeBase implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "KB_NAME", length = 50, nullable = false, unique = true)
    private String name;

    @Column(name = "LINK_UPLOAD")
    private boolean uploadedViaLink = true;

    @Transient
    private List<String> dataFiles;

    @Lob
    @Column(name = "DATA_FILE_LINKS")
    private String dataFileLinksRaw;

    @Transient
    private List<String> ontologyFiles;

    @Lob
    @Column(name = "ONTO_FILE_LINKS")
    private String ontologyFileLinksRaw;

    @Transient
    private boolean kbUploadStatus;

    @Transient
    private List<FileUploadStatus> fileUploadStatus;

    public KnowledgeBase(String name, boolean uploadedViaLink) {
        this.name = name;
        this.uploadedViaLink = uploadedViaLink;
    }

    public KnowledgeBase(String name, boolean uploadedViaLink, String dataFileLinks, String ontologyFileLinks) {
        this.name = name;
        this.uploadedViaLink = uploadedViaLink;
        this.dataFileLinksRaw = dataFileLinks;
        this.ontologyFileLinksRaw = ontologyFileLinks;
    }


    public KnowledgeBase() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isUploadedViaLink() {
        return uploadedViaLink;
    }

    public void setUploadedViaLink(boolean uploadedViaLink) {
        this.uploadedViaLink = uploadedViaLink;
    }

    public List<String> getDataFiles() {
        if(dataFiles == null) {
            dataFiles = getFileListFromRaw(dataFileLinksRaw);
        }
        return dataFiles;
    }

    public void setDataFiles(List<String> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public String getDataFileLinksRaw() {
        return dataFileLinksRaw;
    }

    public void setDataFileLinksRaw(String dataFileLinksRaw) {
        this.dataFileLinksRaw = dataFileLinksRaw;
    }

    public List<String> getOntologyFiles() {
        if(ontologyFiles == null) {
            ontologyFiles = getFileListFromRaw(ontologyFileLinksRaw);
        }
        return ontologyFiles;
    }
    
    private List<String> getFileListFromRaw(String raw) {
        List<String> asList = new ArrayList<>();
        if(raw != null && !raw.trim().isEmpty()) {
            asList = Lists.newArrayList(raw.split("\\n"));
        }
        return asList;
    }

    public void setOntologyFiles(List<String> ontologyFiles) {
        this.ontologyFiles = ontologyFiles;
    }

    public String getOntologyFileLinksRaw() {
        return ontologyFileLinksRaw;
    }

    public void setOntologyFileLinksRaw(String ontologyFileLinksRaw) {
        this.ontologyFileLinksRaw = ontologyFileLinksRaw;
    }

    public boolean isKbUploadStatus() {
        return kbUploadStatus;
    }

    public void setKbUploadStatus(boolean kbUploadStatus) {
        this.kbUploadStatus = kbUploadStatus;
    }

    public List<FileUploadStatus> getFileUploadStatus() {
        return fileUploadStatus;
    }

    public void setFileUploadStatus(List<FileUploadStatus> fileUploadStatus) {
        this.fileUploadStatus = fileUploadStatus;
    }
}
