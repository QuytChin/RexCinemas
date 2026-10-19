package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_settings")
public class LoyaltySettings {
    @Id private Long id;
    @Version @Column(nullable = false) private Long version;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amountPerPoint = BigDecimal.valueOf(10_000);
    @Column(nullable = false) private int minimumPositiveOrderPoints = 1;
    @Column(nullable = false) private boolean voucherStackingAllowed = false;
    @Column(nullable = false) private boolean redemptionEnabled = true;
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    @Column(length = 180) private String updatedBy;
    public Long getId(){return id;} public void setId(Long value){id=value;}
    public Long getVersion(){return version;} public void setVersion(Long value){version=value;}
    public BigDecimal getAmountPerPoint(){return amountPerPoint;} public void setAmountPerPoint(BigDecimal value){amountPerPoint=value;}
    public int getMinimumPositiveOrderPoints(){return minimumPositiveOrderPoints;} public void setMinimumPositiveOrderPoints(int value){minimumPositiveOrderPoints=value;}
    public boolean isVoucherStackingAllowed(){return voucherStackingAllowed;} public void setVoucherStackingAllowed(boolean value){voucherStackingAllowed=value;}
    public boolean isRedemptionEnabled(){return redemptionEnabled;} public void setRedemptionEnabled(boolean value){redemptionEnabled=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String value){updatedBy=value;}
}
