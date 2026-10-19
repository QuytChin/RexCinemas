package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_redemptions", uniqueConstraints = @UniqueConstraint(name = "uk_reward_redemption_request", columnNames = {"user_id", "idempotency_key"}))
public class RewardRedemption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reward_id") private Reward reward;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "voucher_id", unique = true) private Voucher voucher;
    @Column(name = "idempotency_key", nullable = false, length = 80) private String idempotencyKey;
    @Column(nullable = false) private int pointsCost;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private RewardRedemptionStatus status = RewardRedemptionStatus.ISSUED;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime usedAt;
    private LocalDateTime refundedAt;
    public Long getId(){return id;} public void setId(Long value){id=value;}
    public Long getVersion(){return version;} public void setVersion(Long value){version=value;}
    public User getUser(){return user;} public void setUser(User value){user=value;}
    public Reward getReward(){return reward;} public void setReward(Reward value){reward=value;}
    public Voucher getVoucher(){return voucher;} public void setVoucher(Voucher value){voucher=value;}
    public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String value){idempotencyKey=value;}
    public int getPointsCost(){return pointsCost;} public void setPointsCost(int value){pointsCost=value;}
    public RewardRedemptionStatus getStatus(){return status;} public void setStatus(RewardRedemptionStatus value){status=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUsedAt(){return usedAt;} public void setUsedAt(LocalDateTime value){usedAt=value;}
    public LocalDateTime getRefundedAt(){return refundedAt;} public void setRefundedAt(LocalDateTime value){refundedAt=value;}
}
