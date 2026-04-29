package oll.business.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ProcessModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bpmnXml;

    @Column(nullable = false)
    private Integer version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User author;

    @Column
    private LocalDateTime createdAt;

    public ProcessModel() {}

    public ProcessModel(String name, String bpmnXml, User author) {
        this.name = name;
        this.bpmnXml = bpmnXml;
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}