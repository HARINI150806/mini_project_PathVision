package com.pathvision.dto;

import com.pathvision.entity.CollegeCutoff;

public class CollegeCutoffResponse {
    private Long id;
    private Long collegeId;
    private String collegeName;
    private String branch;
    private String branchCode;
    private Integer admissionYear;
    private String community;
    private Double cutoffScore;

    public static CollegeCutoffResponse fromEntity(CollegeCutoff cutoff) {
        CollegeCutoffResponse res = new CollegeCutoffResponse();
        res.setId(cutoff.getId());
        res.setCollegeId(cutoff.getCollege() != null ? cutoff.getCollege().getId() : null);
        res.setCollegeName(cutoff.getCollege() != null ? cutoff.getCollege().getName() : null);
        res.setBranch(cutoff.getBranch());
        res.setBranchCode(cutoff.getBranchCode());
        res.setAdmissionYear(cutoff.getAdmissionYear());
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

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear) {
        this.admissionYear = admissionYear;
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
