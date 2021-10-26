package de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "KNOWLEDGE_BASE_UPLOAD_STATUS")
public class KBUploadStatus implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "KB_ID", nullable = false, unique = true)
    private Long kbId;

    @Column(name = "STATUS")
    private boolean status;

    protected KBUploadStatus() {

    }

    public KBUploadStatus(Long kbId, boolean status) {
        this.kbId = kbId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
