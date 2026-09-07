package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cinema_events")
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(length = 80)
    private String label;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(length = 600)
    private String imageUrl;
    @Column(length = 600)
    private String linkUrl;
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
    public String getLabel(){ return label; }
    public void setLabel(String label){ this.label = label; }
    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description = description; }
    public String getImageUrl(){ return imageUrl; }
    public void setImageUrl(String imageUrl){ this.imageUrl = imageUrl; }
    public String getLinkUrl(){ return linkUrl; }
    public void setLinkUrl(String linkUrl){ this.linkUrl = linkUrl; }
    public int getSortOrder(){ return sortOrder; }
    public void setSortOrder(int sortOrder){ this.sortOrder = sortOrder; }
    public boolean isActive(){ return active; }
    public void setActive(boolean active){ this.active = active; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
}
