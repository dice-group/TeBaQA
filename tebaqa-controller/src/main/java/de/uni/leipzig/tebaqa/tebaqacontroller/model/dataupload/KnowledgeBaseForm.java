package de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.*;

public class KnowledgeBaseForm {

    public enum UploadType {
        URL,
        FILE
    }

    @NotBlank(message = "Please input knowledge base name")
    @Size(min = 2, max = 30, message = "Knowledge base name must be between 2 and 30 characters long")
    @Pattern(regexp = "[a-zA-Z\\d-_]+", message = "Knowledge base name can only contain [a-zA-Z0-9-_]")
    private String name;
    @NotNull(message = "Please choose upload type")
    private UploadType uploadType = UploadType.URL;

    private String dataFileLinks;
    private String ontologyFileLinks;
    private MultipartFile[] dataFiles;
    private MultipartFile[] ontologyFiles;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UploadType getUploadType() {
        return uploadType;
    }

    public void setUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
    }

    public String getDataFileLinks() {
        return dataFileLinks;
    }

    public void setDataFileLinks(String dataFileLinks) {
        this.dataFileLinks = dataFileLinks;
    }

    public String getOntologyFileLinks() {
        return ontologyFileLinks;
    }

    public void setOntologyFileLinks(String ontologyFileLinks) {
        this.ontologyFileLinks = ontologyFileLinks;
    }

    public MultipartFile[] getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(MultipartFile[] dataFiles) {
        this.dataFiles = dataFiles;
    }

    public MultipartFile[] getOntologyFiles() {
        return ontologyFiles;
    }

    public void setOntologyFiles(MultipartFile[] ontologyFiles) {
        this.ontologyFiles = ontologyFiles;
    }

    public boolean isFileUpload() {
        return UploadType.FILE.equals(this.uploadType);
    }

    public boolean isURLUpload() {
        return UploadType.URL.equals(this.uploadType);
    }

    public KnowledgeBase getKnowledgeBaseObject() {
        if (isURLUpload()) {
            return new KnowledgeBase(
                    name == null ? null : name.trim(),
                    true,
                    dataFileLinks == null ? null : dataFileLinks.trim(),
                    ontologyFileLinks == null ? null : ontologyFileLinks.trim());
        }
        else {
            return new KnowledgeBase(name == null ? null : name.trim(), false);
        }
    }
}
