package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes", indexes = {
    @Index(name = "idx_showtimes_auditorium_time", columnList = "auditorium_id, start_time"),
    @Index(name = "idx_showtimes_movie_time", columnList = "movie_id, start_time")
})
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditorium_id")
    private Auditorium auditorium;
    @Column(nullable = false)
    private LocalDateTime startTime;

    // Thêm trường endTime
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie v) {
        this.movie = v;
    }

    public Auditorium getAuditorium() {
        return auditorium;
    }

    public void setAuditorium(Auditorium v) {
        this.auditorium = v;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime v) {
        this.startTime = v;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime v) {
        this.endTime = v;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal v) {
        this.basePrice = v;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        this.active = v;
    }

    // Tự động tính endTime = startTime + thời lượng phim + 15p dọn phòng
    @PrePersist
    @PreUpdate
    public void calculateEndTime() {
        if (startTime != null && movie != null && movie.getDurationMinutes() > 0) {
            this.endTime = startTime.plusMinutes(movie.getDurationMinutes() + 15L);
        }
    }
}
