package com.pathvision.dto;

public class CollegeRecommendationResponse {
    private Long collegeId;
    private String collegeName;
    private String district;
    private String state;
    private String type;
    private Double rating;
    private Double annualFees;
    private String community;
    private Double communityCutoff;
    private Double studentScore;
    private Double distanceKm;
    private Double recommendationScore;
    private String reason;

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

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Double getAnnualFees() {
        return annualFees;
    }

    public void setAnnualFees(Double annualFees) {
        this.annualFees = annualFees;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public Double getCommunityCutoff() {
        return communityCutoff;
    }

    public void setCommunityCutoff(Double communityCutoff) {
        this.communityCutoff = communityCutoff;
    }

    public Double getStudentScore() {
        return studentScore;
    }

    public void setStudentScore(Double studentScore) {
        this.studentScore = studentScore;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getRecommendationScore() {
        return recommendationScore;
    }

    public void setRecommendationScore(Double recommendationScore) {
        this.recommendationScore = recommendationScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
