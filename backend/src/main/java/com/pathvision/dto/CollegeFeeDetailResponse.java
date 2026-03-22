package com.pathvision.dto;

import com.pathvision.entity.College;
import com.pathvision.entity.CollegeFeeDetail;

import java.time.LocalDateTime;
import java.util.List;

public class CollegeFeeDetailResponse {
    private Long collegeId;
    private String collegeName;
    private Double annualFees;
    private String sourceUrl;
    private String syncStatus;
    private String syncMessage;
    private LocalDateTime lastSyncedAt;
    private List<FeeItem> items;

    public static CollegeFeeDetailResponse fromEntity(College college, List<CollegeFeeDetail> details) {
        CollegeFeeDetailResponse response = new CollegeFeeDetailResponse();
        response.setCollegeId(college.getId());
        response.setCollegeName(college.getName());
        response.setAnnualFees(college.getAnnualFees());
        response.setSourceUrl(college.getFeeSourceUrl());
        response.setSyncStatus(college.getFeeSyncStatus());
        response.setSyncMessage(college.getFeeSyncMessage());
        response.setLastSyncedAt(college.getFeeLastSyncedAt());
        response.setItems(details.stream().map(FeeItem::fromEntity).toList());
        return response;
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

    public Double getAnnualFees() {
        return annualFees;
    }

    public void setAnnualFees(Double annualFees) {
        this.annualFees = annualFees;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getSyncMessage() {
        return syncMessage;
    }

    public void setSyncMessage(String syncMessage) {
        this.syncMessage = syncMessage;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public List<FeeItem> getItems() {
        return items;
    }

    public void setItems(List<FeeItem> items) {
        this.items = items;
    }

    public static class FeeItem {
        private String label;
        private String category;
        private Double amount;
        private String amountText;
        private LocalDateTime fetchedAt;

        public static FeeItem fromEntity(CollegeFeeDetail detail) {
            FeeItem item = new FeeItem();
            item.setLabel(detail.getLabel());
            item.setCategory(detail.getCategory());
            item.setAmount(detail.getAmount());
            item.setAmountText(detail.getAmountText());
            item.setFetchedAt(detail.getFetchedAt());
            return item;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getAmountText() {
            return amountText;
        }

        public void setAmountText(String amountText) {
            this.amountText = amountText;
        }

        public LocalDateTime getFetchedAt() {
            return fetchedAt;
        }

        public void setFetchedAt(LocalDateTime fetchedAt) {
            this.fetchedAt = fetchedAt;
        }
    }
}
