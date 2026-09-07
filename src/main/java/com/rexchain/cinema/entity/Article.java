package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 220)
    private String title;
    @Column(length = 80)
    private String category;
    @Column(length = 500)
    private String summary;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(length = 600)
    private String imageUrl;
    @Column(length = 600)
    private String linkUrl;
    private LocalDate publishDate;
    @Column(nullable = false)
    private int sortOrder = 0;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getTitle(){ return title; }
    public void setTitle(String title){ this.title = title; }
    public String getCategory(){ return category; }
    public void setCategory(String category){ this.category = category; }
    public String getSummary(){ return summary; }
    public void setSummary(String summary){ this.summary = summary; }
    public String getContent(){ return content; }
    public void setContent(String content){ this.content = content; }
    public String getImageUrl(){ return imageUrl; }
    public void setImageUrl(String imageUrl){ this.imageUrl = imageUrl; }
    public String getLinkUrl(){ return linkUrl; }
    public void setLinkUrl(String linkUrl){ this.linkUrl = linkUrl; }
    public LocalDate getPublishDate(){ return publishDate; }
    public void setPublishDate(LocalDate publishDate){ this.publishDate = publishDate; }
    public int getSortOrder(){ return sortOrder; }
    public void setSortOrder(int sortOrder){ this.sortOrder = sortOrder; }
    public boolean isActive(){ return active; }
    public void setActive(boolean active){ this.active = active; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
}
