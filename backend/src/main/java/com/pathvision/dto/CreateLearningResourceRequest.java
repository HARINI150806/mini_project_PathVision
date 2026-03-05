package com.pathvision.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateLearningResourceRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String provider;

    @NotBlank
    private String source;

    @NotBlank
    private String level;

    @NotBlank
    private String url;

    @NotBlank
    private String interestKey;

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
}
