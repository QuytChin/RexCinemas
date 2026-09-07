package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="showtimes")
public class Showtime {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="movie_id") private Movie movie;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="auditorium_id") private Auditorium auditorium;
    @Column(nullable=false) private LocalDateTime startTime;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal basePrice;
    @Column(nullable=false) private boolean active=true;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Movie getMovie(){return movie;} public void setMovie(Movie v){this.movie=v;}
    public Auditorium getAuditorium(){return auditorium;} public void setAuditorium(Auditorium v){this.auditorium=v;}
    public LocalDateTime getStartTime(){return startTime;} public void setStartTime(LocalDateTime v){this.startTime=v;}
    public BigDecimal getBasePrice(){return basePrice;} public void setBasePrice(BigDecimal v){this.basePrice=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){this.active=v;}
}
