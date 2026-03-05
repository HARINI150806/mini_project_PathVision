package com.pathvision.dto;

import com.pathvision.entity.College;

public class CollegeResponse {
    private Long id;
    private String name;
    private String type;
    private String district;
    private String state;
    private String address;
    private Double rating;
    private Double annualFees;
    private Double communityCutoff;
    private Double latitude;
    private Double longitude;

    public static CollegeResponse fromEntity(College college) {
        CollegeResponse response = new CollegeResponse();
        response.setId(college.getId());
        response.setName(college.getName());
        response.setType(college.getType());
        response.setDistrict(college.getDistrict());
        response.setState(college.getState());
        response.setAddress(college.getAddress());
        response.setRating(college.getRating());
        response.setAnnualFees(college.getAnnualFees());
        response.setLatitude(college.getLatitude());
        response.setLongitude(college.getLongitude());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAnnualFees() {
        return annualFees;
    }

    public void setAnnualFees(Double annualFees) {
        this.annualFees = annualFees;
    }

    public Double getCommunityCutoff() {
        return communityCutoff;
    }

    public void setCommunityCutoff(Double communityCutoff) {
        this.communityCutoff = communityCutoff;
    }
}
