package com.pathvision.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "student_profiles")
public class StudentProfile {
    // CS cutoff only
    @Column(name = "cs_cutoff")
    private Double csCutoff;
    public Double getCsCutoff() { return csCutoff; }
    public void setCsCutoff(Double csCutoff) { this.csCutoff = csCutoff; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Minimal required fields
    @Lob
    @Column(name = "interests_json")
    private String interestsJson; // JSON array of interests

    @Column(name = "marksheet_url")
    private String marksheetUrl; // stored file URL/path
    
    @Lob
    @Column(name = "marksheet_text", columnDefinition = "LONGTEXT")
    private String marksheetText; // OCRed marksheet text
    
    @Column(name = "aggregate_percentage")
    private Double aggregatePercentage; // Computed aggregate percentage

    // Address split
    @Column(name = "address_line")
    private String addressLine;

    private String city;
    private String state;
    private String pincode;

    // Stream: CSE, Arts, Biology, etc.
    @Column(name = "stream")
    private String stream;

    // Gender and phone
    private String gender; // MALE/FEMALE/OTHER/PREFER_NOT_TO_SAY
    private String phone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public StudentProfile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getInterestsJson() { return interestsJson; }
    public void setInterestsJson(String interestsJson) { this.interestsJson = interestsJson; }

    public String getMarksheetUrl() { return marksheetUrl; }
    public void setMarksheetUrl(String marksheetUrl) { this.marksheetUrl = marksheetUrl; }

    public String getMarksheetText() { return marksheetText; }
    public void setMarksheetText(String marksheetText) { this.marksheetText = marksheetText; }

    public Double getAggregatePercentage() { return aggregatePercentage; }
    public void setAggregatePercentage(Double aggregatePercentage) { this.aggregatePercentage = aggregatePercentage; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getStream() { return stream; }
    public void setStream(String stream) { this.stream = stream; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
