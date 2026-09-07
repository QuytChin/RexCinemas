package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="booking_seats")
public class BookingSeat {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="booking_id") private Booking booking;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="showtime_seat_id") private ShowtimeSeat showtimeSeat;
    @Column(nullable=false, length=15) private String seatLabel;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal price;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Booking getBooking(){return booking;} public void setBooking(Booking v){this.booking=v;}
    public ShowtimeSeat getShowtimeSeat(){return showtimeSeat;} public void setShowtimeSeat(ShowtimeSeat v){this.showtimeSeat=v;}
    public String getSeatLabel(){return seatLabel;} public void setSeatLabel(String v){this.seatLabel=v;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){this.price=v;}
}
