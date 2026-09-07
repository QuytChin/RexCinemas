package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(length = 30)
    private String phone;
    @Column(length = 600)
    private String avatarUrl;
    @Column(length = 300)
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role = Role.CUSTOMER;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(nullable = false)
    private int loyaltyPoints = 0;
    @Column(nullable = false)
    private int lifetimePoints = 0;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getFullName(){ return fullName; }
    public void setFullName(String fullName){ this.fullName = fullName; }
    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }
    public String getPassword(){ return password; }
    public void setPassword(String password){ this.password = password; }
    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone = phone; }
    public String getAvatarUrl(){ return avatarUrl; }
    public void setAvatarUrl(String avatarUrl){ this.avatarUrl = avatarUrl; }
    public String getAddress(){ return address; }
    public void setAddress(String address){ this.address = address; }
    public Role getRole(){ return role; }
    public void setRole(Role role){ this.role = role; }
    public boolean isEnabled(){ return enabled; }
    public void setEnabled(boolean enabled){ this.enabled = enabled; }
    public int getLoyaltyPoints(){ return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints){ this.loyaltyPoints = Math.max(0, loyaltyPoints); }
    public int getLifetimePoints(){ return lifetimePoints; }
    public void setLifetimePoints(int lifetimePoints){ this.lifetimePoints = Math.max(0, lifetimePoints); }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    @Transient
    public MembershipTier getMembershipTier(){
        if (lifetimePoints >= 1200) return MembershipTier.DIAMOND;
        if (lifetimePoints >= 600) return MembershipTier.GOLD;
        if (lifetimePoints >= 250) return MembershipTier.SILVER;
        return MembershipTier.MEMBER;
    }

    @Transient
    public int getNextTierTarget(){
        if (lifetimePoints < 250) return 250;
        if (lifetimePoints < 600) return 600;
        if (lifetimePoints < 1200) return 1200;
        return 1200;
    }

    @Transient
    public int getTierProgressPercent(){
        int start;
        int target;
        if (lifetimePoints < 250) { start = 0; target = 250; }
        else if (lifetimePoints < 600) { start = 250; target = 600; }
        else if (lifetimePoints < 1200) { start = 600; target = 1200; }
        else return 100;
        return Math.min(100, Math.max(0, (int)Math.round((lifetimePoints - start) * 100.0 / (target - start))));
    }
}
