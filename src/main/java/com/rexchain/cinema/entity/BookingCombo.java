package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="booking_combos")
public class BookingCombo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="booking_id") private Booking booking;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="combo_id") private ComboProduct combo;
    @Column(nullable=false, length=120) private String comboName;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal unitPrice;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal lineTotal;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Booking getBooking(){return booking;} public void setBooking(Booking v){booking=v;}
    public ComboProduct getCombo(){return combo;} public void setCombo(ComboProduct v){combo=v;}
    public String getComboName(){return comboName;} public void setComboName(String v){comboName=v;}
    public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;}
    public BigDecimal getLineTotal(){return lineTotal;} public void setLineTotal(BigDecimal v){lineTotal=v;}
}
