package com.pathvision.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "college_cutoffs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"college_id", "community"})
)
public class CollegeCutoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @NotBlank(message = "Community is required")
    private String community;

    @NotNull(message = "Cutoff score is required")
    private Double cutoffScore;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public College getCollege() {
        return college;
    }

    public void setCollege(College college) {
        this.college = college;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public Double getCutoffScore() {
        return cutoffScore;
    }

    public void setCutoffScore(Double cutoffScore) {
        this.cutoffScore = cutoffScore;
    }
}
