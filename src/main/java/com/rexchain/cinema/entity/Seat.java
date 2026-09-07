package com.rexchain.cinema.entity;

import jakarta.persistence.*;

@Entity
@Table(name="seats", uniqueConstraints=@UniqueConstraint(columnNames={"auditorium_id","seat_row","seat_number"}))
public class Seat {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="auditorium_id") private Auditorium auditorium;
    @Column(name="seat_row", nullable=false, length=5) private String seatRow;
    @Column(name="seat_number", nullable=false) private int seatNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private SeatType type=SeatType.STANDARD;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Auditorium getAuditorium(){return auditorium;} public void setAuditorium(Auditorium v){this.auditorium=v;}
    public String getSeatRow(){return seatRow;} public void setSeatRow(String v){this.seatRow=v;}
    public int getSeatNumber(){return seatNumber;} public void setSeatNumber(int v){this.seatNumber=v;}
    public SeatType getType(){return type;} public void setType(SeatType v){this.type=v;}
    public String getLabel(){return seatRow+seatNumber;}
}
