package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_tier_policies", uniqueConstraints = @UniqueConstraint(name = "uk_membership_tier_policy_tier", columnNames = "tier"))
public class MembershipTierPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private Long version;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private MembershipTier tier;
    @Column(nullable = false) private int qualifyingThreshold;
    @Column(nullable = false, precision = 7, scale = 4) private BigDecimal ticketDiscountPercent = BigDecimal.ZERO;
    @Column(nullable = false, precision = 7, scale = 4) private BigDecimal pointMultiplier = BigDecimal.ONE;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    @Column(length = 180) private String updatedBy;
    public Long getId(){return id;} public void setId(Long value){id=value;}
    public Long getVersion(){return version;} public void setVersion(Long value){version=value;}
    public MembershipTier getTier(){return tier;} public void setTier(MembershipTier value){tier=value;}
    public int getQualifyingThreshold(){return qualifyingThreshold;} public void setQualifyingThreshold(int value){qualifyingThreshold=value;}
    public BigDecimal getTicketDiscountPercent(){return ticketDiscountPercent;} public void setTicketDiscountPercent(BigDecimal value){ticketDiscountPercent=value;}
    public BigDecimal getPointMultiplier(){return pointMultiplier;} public void setPointMultiplier(BigDecimal value){pointMultiplier=value;}
    public boolean isActive(){return active;} public void setActive(boolean value){active=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String value){updatedBy=value;}
}
