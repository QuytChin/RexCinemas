package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_rewards")
public class Reward {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private Long version;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 600) private String description;
    @Column(nullable = false) private int pointsCost;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal voucherAmount;
    @Column(precision = 12, scale = 2) private BigDecimal voucherMinOrderAmount = BigDecimal.ZERO;
    @Column(nullable = false) private int validityDays = 30;
    private Integer redemptionLimit;
    @Column(nullable = false) private int redeemedCount = 0;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    public Long getId(){return id;} public void setId(Long value){id=value;}
    public Long getVersion(){return version;} public void setVersion(Long value){version=value;}
    public String getName(){return name;} public void setName(String value){name=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public int getPointsCost(){return pointsCost;} public void setPointsCost(int value){pointsCost=value;}
    public BigDecimal getVoucherAmount(){return voucherAmount;} public void setVoucherAmount(BigDecimal value){voucherAmount=value;}
    public BigDecimal getVoucherMinOrderAmount(){return voucherMinOrderAmount;} public void setVoucherMinOrderAmount(BigDecimal value){voucherMinOrderAmount=value;}
    public int getValidityDays(){return validityDays;} public void setValidityDays(int value){validityDays=value;}
    public Integer getRedemptionLimit(){return redemptionLimit;} public void setRedemptionLimit(Integer value){redemptionLimit=value;}
    public int getRedeemedCount(){return redeemedCount;} public void setRedeemedCount(int value){redeemedCount=value;}
    public boolean isActive(){return active;} public void setActive(boolean value){active=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
