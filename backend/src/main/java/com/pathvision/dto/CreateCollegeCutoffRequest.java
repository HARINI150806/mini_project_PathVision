package com.pathvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCollegeCutoffRequest {

    @NotBlank(message = "Community is required")
    private String community;

    @NotNull(message = "Cutoff score is required")
    private Double cutoffScore;

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
