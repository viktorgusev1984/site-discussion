package com.example.discussions.model;
import jakarta.persistence.*;
@Entity @Table(name="categories") public class Category {@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;@Column(nullable=false,unique=true) public String name;@Column(nullable=false,unique=true) public String slug;public String description;@Column(nullable=false) public String color;}
