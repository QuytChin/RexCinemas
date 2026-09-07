package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="showtime_seats", uniqueConstraints=@UniqueConstraint(columnNames={"showtime_id","seat_id"}))
public class ShowtimeSeat {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="showtime_id") private Showtime showtime;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="seat_id") private Seat seat;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private SeatStatus status=SeatStatus.AVAILABLE;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal price;
    private Long heldByUserId;
    private LocalDateTime holdUntil;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getVersion(){return version;} public void setVersion(Long v){this.version=v;}
    public Showtime getShowtime(){return showtime;} public void setShowtime(Showtime v){this.showtime=v;}
    public Seat getSeat(){return seat;} public void setSeat(Seat v){this.seat=v;}
    public SeatStatus getStatus(){return status;} public void setStatus(SeatStatus v){this.status=v;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){this.price=v;}
    public Long getHeldByUserId(){return heldByUserId;} public void setHeldByUserId(Long v){this.heldByUserId=v;}
    public LocalDateTime getHoldUntil(){return holdUntil;} public void setHoldUntil(LocalDateTime v){this.holdUntil=v;}
}
