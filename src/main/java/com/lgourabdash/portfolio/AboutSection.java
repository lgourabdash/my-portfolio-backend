package com.lgourabdash.portfolio;

import jakarta.persistence.*;

@Entity
@Table(name = "about_section")
public class AboutSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
