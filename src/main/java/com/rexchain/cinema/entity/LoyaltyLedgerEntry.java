package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_ledger", uniqueConstraints = @UniqueConstraint(name = "uk_loyalty_ledger_reference", columnNames = "reference_key"), indexes = {
        @Index(name = "idx_loyalty_ledger_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_loyalty_ledger_type_created", columnList = "transaction_type,created_at")
})
public class LoyaltyLedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "booking_id") private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "redemption_id") private RewardRedemption redemption;
    @Enumerated(EnumType.STRING) @Column(name = "transaction_type", nullable = false, length = 40) private LoyaltyTransactionType transactionType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private LoyaltySource source;
    @Column(name = "reference_key", nullable = false, length = 120) private String reference;
    @Column(nullable = false) private int pointsDelta;
    @Column(nullable = false) private int availableBefore;
    @Column(nullable = false) private int availableAfter;
    @Column(nullable = false) private int qualifyingDelta;
    @Column(nullable = false) private int qualifyingBefore;
    @Column(nullable = false) private int qualifyingAfter;
    @Column(nullable = false) private int lifetimeDelta;
    @Column(nullable = false) private int lifetimeBefore;
    @Column(nullable = false) private int lifetimeAfter;
    @Column(nullable = false) private int redeemedDelta;
    @Column(nullable = false) private int reversedDelta;
    @Column(length = 800) private String reason;
    @Column(length = 180) private String actor;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId(){return id;} public void setId(Long value){id=value;}
    public User getUser(){return user;} public void setUser(User value){user=value;}
    public Booking getBooking(){return booking;} public void setBooking(Booking value){booking=value;}
    public RewardRedemption getRedemption(){return redemption;} public void setRedemption(RewardRedemption value){redemption=value;}
    public LoyaltyTransactionType getTransactionType(){return transactionType;} public void setTransactionType(LoyaltyTransactionType value){transactionType=value;}
    public LoyaltySource getSource(){return source;} public void setSource(LoyaltySource value){source=value;}
    public String getReference(){return reference;} public void setReference(String value){reference=value;}
    public int getPointsDelta(){return pointsDelta;} public void setPointsDelta(int value){pointsDelta=value;}
    public int getAvailableBefore(){return availableBefore;} public void setAvailableBefore(int value){availableBefore=value;}
    public int getAvailableAfter(){return availableAfter;} public void setAvailableAfter(int value){availableAfter=value;}
    public int getQualifyingDelta(){return qualifyingDelta;} public void setQualifyingDelta(int value){qualifyingDelta=value;}
    public int getQualifyingBefore(){return qualifyingBefore;} public void setQualifyingBefore(int value){qualifyingBefore=value;}
    public int getQualifyingAfter(){return qualifyingAfter;} public void setQualifyingAfter(int value){qualifyingAfter=value;}
    public int getLifetimeDelta(){return lifetimeDelta;} public void setLifetimeDelta(int value){lifetimeDelta=value;}
    public int getLifetimeBefore(){return lifetimeBefore;} public void setLifetimeBefore(int value){lifetimeBefore=value;}
    public int getLifetimeAfter(){return lifetimeAfter;} public void setLifetimeAfter(int value){lifetimeAfter=value;}
    public int getRedeemedDelta(){return redeemedDelta;} public void setRedeemedDelta(int value){redeemedDelta=value;}
    public int getReversedDelta(){return reversedDelta;} public void setReversedDelta(int value){reversedDelta=value;}
    public String getReason(){return reason;} public void setReason(String value){reason=value;}
    public String getActor(){return actor;} public void setActor(String value){actor=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
}
