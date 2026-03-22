package com.pathvision.dto;

import jakarta.validation.constraints.NotBlank;

public class ConfigureCollegeFeeSourceRequest {

    @NotBlank(message = "Fee source URL is required")
    private String sourceUrl;

    private String sourceType;
    private String rowSelector;
    private String labelSelector;
    private String amountSelector;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getRowSelector() {
        return rowSelector;
    }

    public void setRowSelector(String rowSelector) {
        this.rowSelector = rowSelector;
    }

    public String getLabelSelector() {
        return labelSelector;
    }

    public void setLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
    }

    public String getAmountSelector() {
        return amountSelector;
    }

    public void setAmountSelector(String amountSelector) {
        this.amountSelector = amountSelector;
    }
}
