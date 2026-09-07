package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="movies")
public class Movie {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false, columnDefinition="TEXT") private String description;
    @Column(nullable=false) private int durationMinutes;
    @Column(length=20) private String ageRating;
    @Column(length=120) private String genre;
    @Column(length=80) private String language;
    private LocalDate releaseDate;
    @Column(length=600) private String posterUrl;
    @Column(length=600) private String trailerUrl;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTitle(){return title;} public void setTitle(String v){this.title=v;}
    public String getDescription(){return description;} public void setDescription(String v){this.description=v;}
    public int getDurationMinutes(){return durationMinutes;} public void setDurationMinutes(int v){this.durationMinutes=v;}
    public String getAgeRating(){return ageRating;} public void setAgeRating(String v){this.ageRating=v;}
    public String getGenre(){return genre;} public void setGenre(String v){this.genre=v;}
    public String getLanguage(){return language;} public void setLanguage(String v){this.language=v;}
    public LocalDate getReleaseDate(){return releaseDate;} public void setReleaseDate(LocalDate v){this.releaseDate=v;}
    public String getPosterUrl(){return posterUrl;} public void setPosterUrl(String v){this.posterUrl=v;}
    public String getTrailerUrl(){return trailerUrl;} public void setTrailerUrl(String v){this.trailerUrl=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){this.active=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){this.createdAt=v;}
}
