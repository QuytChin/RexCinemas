package com.rexchain.cinema.entity;

import jakarta.persistence.*;

@Entity
@Table(name="auditoriums")
public class Auditorium {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=80) private String name;
    @Column(nullable=false) private int totalRows;
    @Column(nullable=false) private int seatsPerRow;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="cinema_id") private Cinema cinema;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getName(){return name;} public void setName(String v){this.name=v;}
    public int getTotalRows(){return totalRows;} public void setTotalRows(int v){this.totalRows=v;}
    public int getSeatsPerRow(){return seatsPerRow;} public void setSeatsPerRow(int v){this.seatsPerRow=v;}
    public Cinema getCinema(){return cinema;} public void setCinema(Cinema v){this.cinema=v;}
}
