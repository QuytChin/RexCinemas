package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_items")
public class MediaItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 180)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type = MediaType.TRAILER;
    @Column(length = 600)
    private String thumbnailUrl;
    @Column(nullable = false, length = 800)
    private String videoUrl;
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
    public MediaType getType(){ return type; }
    public void setType(MediaType type){ this.type = type; }
    public String getThumbnailUrl(){ return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl){ this.thumbnailUrl = thumbnailUrl; }
    public String getVideoUrl(){ return videoUrl; }
    public void setVideoUrl(String videoUrl){ this.videoUrl = videoUrl; }
    public int getSortOrder(){ return sortOrder; }
    public void setSortOrder(int sortOrder){ this.sortOrder = sortOrder; }
    public boolean isActive(){ return active; }
    public void setActive(boolean active){ this.active = active; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
}
