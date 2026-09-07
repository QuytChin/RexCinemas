package com.rexchain.cinema.entity;

import jakarta.persistence.*;

@Entity
@Table(name="cinemas")
public class Cinema {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, length=100) private String city;
    @Column(nullable=false, length=255) private String address;
    @Column(length=30) private String phone;
    @Column(nullable=false) private boolean active=true;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getName(){return name;} public void setName(String v){this.name=v;}
    public String getCity(){return city;} public void setCity(String v){this.city=v;}
    public String getAddress(){return address;} public void setAddress(String v){this.address=v;}
    public String getPhone(){return phone;} public void setPhone(String v){this.phone=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){this.active=v;}
}
