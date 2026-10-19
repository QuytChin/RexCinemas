package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_policy_audit", indexes = @Index(name = "idx_loyalty_policy_audit_created", columnList = "created_at"))
public class LoyaltyPolicyAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 50) private String policyType;
    @Column(nullable = false, length = 100) private String policyKey;
    @Column(length = 4000) private String beforeValue;
    @Column(length = 4000) private String afterValue;
    @Column(nullable = false, length = 180) private String actor;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId(){return id;} public void setId(Long value){id=value;}
    public String getPolicyType(){return policyType;} public void setPolicyType(String value){policyType=value;}
    public String getPolicyKey(){return policyKey;} public void setPolicyKey(String value){policyKey=value;}
    public String getBeforeValue(){return beforeValue;} public void setBeforeValue(String value){beforeValue=value;}
    public String getAfterValue(){return afterValue;} public void setAfterValue(String value){afterValue=value;}
    public String getActor(){return actor;} public void setActor(String value){actor=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
}
