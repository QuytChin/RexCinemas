package com.rexchain.cinema.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users", uniqueConstraints =
        @UniqueConstraint(name = "uk_app_users_email", columnNames = "email"), indexes = {
        @Index(name = "idx_app_users_favorite_cinema", columnList = "favorite_cinema_id")
})
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version @Column(nullable = false)
    private Long version;
    @Column(nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(length = 50)
    private String username;
    private LocalDate birthDate;
    @Column(name = "national_id", length = 12)
    private String nationalId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favorite_cinema_id")
    private Cinema favoriteCinema;
    @Column(nullable = false)
    private String password;
    @Column(length = 30)
    private String phone;
    @Column(length = 20)
    private String gender;
    @Column(length = 600)
    private String avatarUrl;
    @Column(length = 300)
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role = Role.CUSTOMER;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "credential_version", nullable = false)
    private int credentialVersion = 0;
    @Column(nullable = false)
    private int loyaltyPoints = 0;
    @Column(nullable = false)
    private int lifetimePoints = 0;
    @Column(nullable = false)
    private int tierQualifyingPoints = 0;
    @Column(nullable = false)
    private int redeemedPoints = 0;
    @Column(nullable = false)
    private int reversedPoints = 0;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public Long getVersion(){ return version; }
    public void setVersion(Long version){ this.version = version; }
    public String getFullName(){ return fullName; }
    public void setFullName(String fullName){ this.fullName = fullName; }
    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }
    public String getUsername(){ return username; }
    public void setUsername(String username){ this.username = username; }
    public LocalDate getBirthDate(){ return birthDate; }
    public void setBirthDate(LocalDate birthDate){ this.birthDate = birthDate; }
    public String getNationalId(){ return nationalId; }
    public void setNationalId(String nationalId){ this.nationalId = nationalId; }
    public Cinema getFavoriteCinema(){ return favoriteCinema; }
    public void setFavoriteCinema(Cinema favoriteCinema){ this.favoriteCinema = favoriteCinema; }
    public String getPassword(){ return password; }
    public void setPassword(String password){ this.password = password; }
    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone = phone; }
    public String getGender(){ return gender; }
    public void setGender(String gender){ this.gender = gender; }
    public String getAvatarUrl(){ return avatarUrl; }
    public void setAvatarUrl(String avatarUrl){ this.avatarUrl = avatarUrl; }
    public String getAddress(){ return address; }
    public void setAddress(String address){ this.address = address; }
    public Role getRole(){ return role; }
    public void setRole(Role role){ this.role = role; }
    public boolean isEnabled(){ return enabled; }
    public void setEnabled(boolean enabled){ this.enabled = enabled; }
    public int getCredentialVersion(){ return credentialVersion; }
    public void setCredentialVersion(int credentialVersion){ this.credentialVersion = Math.max(0, credentialVersion); }
    public int getLoyaltyPoints(){ return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints){ this.loyaltyPoints = Math.max(0, loyaltyPoints); }
    public int getLifetimePoints(){ return lifetimePoints; }
    public void setLifetimePoints(int lifetimePoints){ this.lifetimePoints = Math.max(0, lifetimePoints); }
    public int getTierQualifyingPoints(){ return tierQualifyingPoints; }
    public void setTierQualifyingPoints(int value){ this.tierQualifyingPoints = Math.max(0, value); }
    public int getRedeemedPoints(){ return redeemedPoints; }
    public void setRedeemedPoints(int value){ this.redeemedPoints = Math.max(0, value); }
    public int getReversedPoints(){ return reversedPoints; }
    public void setReversedPoints(int value){ this.reversedPoints = Math.max(0, value); }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    @Transient
    public MembershipTier getMembershipTier(){
        int points = tierQualifyingPoints > 0 ? tierQualifyingPoints : lifetimePoints;
        if (points >= 1200) return MembershipTier.DIAMOND;
        if (points >= 600) return MembershipTier.GOLD;
        if (points >= 250) return MembershipTier.SILVER;
        return MembershipTier.MEMBER;
    }

    @Transient
    public int getNextTierTarget(){
        int points = tierQualifyingPoints > 0 ? tierQualifyingPoints : lifetimePoints;
        if (points < 250) return 250;
        if (points < 600) return 600;
        if (points < 1200) return 1200;
        return 1200;
    }

    @Transient
    public int getTierProgressPercent(){
        int start;
        int target;
        int points = tierQualifyingPoints > 0 ? tierQualifyingPoints : lifetimePoints;
        if (points < 250) { start = 0; target = 250; }
        else if (points < 600) { start = 250; target = 600; }
        else if (points < 1200) { start = 600; target = 1200; }
        else return 100;
        return Math.min(100, Math.max(0, (int)Math.round((points - start) * 100.0 / (target - start))));
    }
}
