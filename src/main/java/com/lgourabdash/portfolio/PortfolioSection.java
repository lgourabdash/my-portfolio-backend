package com.lgourabdash.portfolio;

import jakarta.persistence.*;

@Entity
@Table(name = "portfolio_section")
public class PortfolioSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_key", nullable = false, unique = true, length = 64)
    private String sectionKey;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public void setSectionKey(String sectionKey) {
        this.sectionKey = sectionKey;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
