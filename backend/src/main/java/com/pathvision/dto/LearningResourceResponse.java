package com.pathvision.dto;

import com.pathvision.entity.LearningResource;

public class LearningResourceResponse {
    private Long id;
    private String title;
    private String provider;
    private String source;
    private String level;
    private String url;
    private String interestKey;
    private String interestLabel;
    private String category;
    private Integer matchPercentage;
    private String confidenceText;

    public static LearningResourceResponse fromEntity(LearningResource resource) {
        LearningResourceResponse response = new LearningResourceResponse();
        response.setId(resource.getId());
        response.setTitle(resource.getTitle());
        response.setProvider(resource.getProvider());
        response.setSource(resource.getSource());
        response.setLevel(resource.getLevel());
        response.setUrl(resource.getUrl());
        response.setInterestKey(resource.getInterestKey());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInterestKey() {
        return interestKey;
    }

    public void setInterestKey(String interestKey) {
        this.interestKey = interestKey;
    }

    public String getInterestLabel() {
        return interestLabel;
    }

    public void setInterestLabel(String interestLabel) {
        this.interestLabel = interestLabel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(Integer matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public String getConfidenceText() {
        return confidenceText;
    }

    public void setConfidenceText(String confidenceText) {
        this.confidenceText = confidenceText;
    }
}
