package de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload;


import com.fasterxml.jackson.annotation.JsonIgnore;
import de.uni.leipzig.tebaqa.tebaqacontroller.validation.ValidationUtils;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "FILE_UPLOAD_STATUS",
        uniqueConstraints =
        @UniqueConstraint(columnNames = {"KB_ID", "LINK"})
)
public class FileUploadStatus implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long id;

    @Column(name = "KB_ID", nullable = false)
    @JsonIgnore
    private Long kbId;

    @Lob
    @Column(name = "LINK", nullable = false)
    private String link;

    @Column(name = "STATUS")
    private int status;
    // 0 = Processing
    // 1 = Success
    // 2 = Failure

    protected FileUploadStatus() {

    }

    public FileUploadStatus(Long kbId, String link) {
        this.kbId = kbId;
        this.link = link;
        this.status = 0;
    }

    public FileUploadStatus(Long kbId, String link, int status) {
        this.kbId = kbId;
        this.link = link;
        this.status = status;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public String getFileName() {
        return ValidationUtils.getFileName(link);
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
