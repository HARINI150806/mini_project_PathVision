package com.pathvision.dto;

import com.pathvision.entity.CollegeCutoff;

public class CollegeCutoffResponse {
    private Long id;
    private Long collegeId;
    private String collegeName;
    private String community;
    private Double cutoffScore;

    public static CollegeCutoffResponse fromEntity(CollegeCutoff cutoff) {
        CollegeCutoffResponse res = new CollegeCutoffResponse();
        res.setId(cutoff.getId());
        res.setCollegeId(cutoff.getCollege() != null ? cutoff.getCollege().getId() : null);
        res.setCollegeName(cutoff.getCollege() != null ? cutoff.getCollege().getName() : null);
        res.setCommunity(cutoff.getCommunity());
        res.setCutoffScore(cutoff.getCutoffScore());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
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
