package com.sast.readtrack.model;
import java.time.LocalDateTime;
public class Book {
    private Long id;
    private String title;
    private String author;
    private Integer totalPages;
    private Integer readPages;
    private String status;
    private Long userId;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getAuthor() { return author; }
    public void setAuthor(String v) { author = v; }
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer v) { totalPages = v; }
    public Integer getReadPages() { return readPages; }
    public void setReadPages(Integer v) { readPages = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
