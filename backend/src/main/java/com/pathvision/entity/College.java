package com.pathvision.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "colleges")
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "College name is required")
    private String name;

    @NotBlank(message = "College type is required")
    private String type;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Address is required")
    private String address;

    private Double rating;
    private Double annualFees;
    private String feeSourceUrl;
    private String feeSourceType;
    private String feeRowSelector;
    private String feeLabelSelector;
    private String feeAmountSelector;
    private LocalDateTime feeLastSyncedAt;
    private String feeSyncStatus;

    @Column(length = 1000)
    private String feeSyncMessage;

    @Column(nullable = true)
    private Double latitude;

    @Column(nullable = true)
    private Double longitude;

    public College() {
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

    public String getFeeSourceUrl() {
        return feeSourceUrl;
    }

    public void setFeeSourceUrl(String feeSourceUrl) {
        this.feeSourceUrl = feeSourceUrl;
    }

    public String getFeeSourceType() {
        return feeSourceType;
    }

    public void setFeeSourceType(String feeSourceType) {
        this.feeSourceType = feeSourceType;
    }

    public String getFeeRowSelector() {
        return feeRowSelector;
    }

    public void setFeeRowSelector(String feeRowSelector) {
        this.feeRowSelector = feeRowSelector;
    }

    public String getFeeLabelSelector() {
        return feeLabelSelector;
    }

    public void setFeeLabelSelector(String feeLabelSelector) {
        this.feeLabelSelector = feeLabelSelector;
    }

    public String getFeeAmountSelector() {
        return feeAmountSelector;
    }

    public void setFeeAmountSelector(String feeAmountSelector) {
        this.feeAmountSelector = feeAmountSelector;
    }

    public LocalDateTime getFeeLastSyncedAt() {
        return feeLastSyncedAt;
    }

    public void setFeeLastSyncedAt(LocalDateTime feeLastSyncedAt) {
        this.feeLastSyncedAt = feeLastSyncedAt;
    }

    public String getFeeSyncStatus() {
        return feeSyncStatus;
    }

    public void setFeeSyncStatus(String feeSyncStatus) {
        this.feeSyncStatus = feeSyncStatus;
    }

    public String getFeeSyncMessage() {
        return feeSyncMessage;
    }

    public void setFeeSyncMessage(String feeSyncMessage) {
        this.feeSyncMessage = feeSyncMessage;
    }
}
