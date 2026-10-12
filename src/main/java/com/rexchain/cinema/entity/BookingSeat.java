package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="booking_seats", indexes = {
    @Index(name = "idx_booking_seats_qr_token", columnList = "qr_token"),
    @Index(name = "idx_booking_seats_ticket_code", columnList = "ticket_code"),
    @Index(name = "idx_booking_seats_booking", columnList = "booking_id")
})
public class BookingSeat {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="booking_id") private Booking booking;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="showtime_seat_id") private ShowtimeSeat showtimeSeat;
    @Column(nullable=false, length=15) private String seatLabel;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal price;

    /** Individual ticket attributes (Phân đoạn 4 - Vé điện tử độc lập). */
    @Column(name = "ticket_code", length = 40)
    private String ticketCode;

    @Column(name = "qr_token", unique = true, length = 80)
    private String qrToken;

    @Column(nullable = false)
    private boolean checkedIn = false;

    private LocalDateTime checkedInAt;

    @Column(length = 180)
    private String checkedInBy;

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Booking getBooking(){return booking;} public void setBooking(Booking v){this.booking=v;}
    public ShowtimeSeat getShowtimeSeat(){return showtimeSeat;} public void setShowtimeSeat(ShowtimeSeat v){this.showtimeSeat=v;}
    public String getSeatLabel(){return seatLabel;} public void setSeatLabel(String v){this.seatLabel=v;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){this.price=v;}

    public String getTicketCode(){return ticketCode;} public void setTicketCode(String v){this.ticketCode=v;}
    public String getQrToken(){return qrToken;} public void setQrToken(String v){this.qrToken=v;}
    public boolean isCheckedIn(){return checkedIn || checkedInAt != null;} public void setCheckedIn(boolean v){this.checkedIn=v;}
    public LocalDateTime getCheckedInAt(){return checkedInAt;} public void setCheckedInAt(LocalDateTime v){this.checkedInAt=v;}
    public String getCheckedInBy(){return checkedInBy;} public void setCheckedInBy(String v){this.checkedInBy=v;}
}
