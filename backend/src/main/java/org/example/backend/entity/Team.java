package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    private String specialty;
    private String capacity;
    private String region;
    private String factoryType;

    @Column(name = "capacity_value")
    private Double capacityValue;

    @Column(name = "capacity_unit")
    private String capacityUnit;

    @Column(name = "factory_image_url", columnDefinition = "TEXT")
    private String factoryImageUrl;

    @Column(name = "factory_images", columnDefinition = "TEXT")
    private String factoryImages;

    @Column(name = "verification_status")
    private String verificationStatus = "NOT_SUBMITTED";

    @Column(name = "business_license", length = 1000)
    private String businessLicense;

    @Column(name = "business_address", length = 1000)
    private String businessAddress;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(name = "facebook_url", length = 1000)
    private String facebookUrl;

    @Column(name = "certification_document", length = 1000)
    private String certificationDocument;

    @Column(name = "certificates", length = 1000)
    private String certificates;

    @Column(name = "verification_reject_reason", length = 1000)
    private String verificationRejectReason;

    @Column(name = "completed_orders", nullable = false, columnDefinition = "integer default 0")
    private int completedOrders = 0;

    @Column(name = "cancelled_orders", nullable = false, columnDefinition = "integer default 0")
    private int cancelledOrders = 0;

    @Column(name = "total_orders", nullable = false, columnDefinition = "integer default 0")
    private int totalOrders = 0;

    @Column(name = "on_time_orders", nullable = false, columnDefinition = "integer default 0")
    private int onTimeOrders = 0;

    @Column(name = "late_orders", nullable = false, columnDefinition = "integer default 0")
    private int lateOrders = 0;

    @Column(name = "total_ratings", nullable = false, columnDefinition = "integer default 0")
    private int totalRatings = 0;

    @Column(name = "sum_ratings", nullable = false, columnDefinition = "double default 0.0")
    private double sumRatings = 0.0;

    @Column(name = "invite_code", unique = true, length = 6)
    private String inviteCode;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.inviteCode == null || this.inviteCode.isEmpty()) {
            this.inviteCode = generateInviteCode();
        }
    }

    private static String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public Team() {
    }

    // === Getters & Setters ===
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean isPublished) {
        this.isPublished = isPublished;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getFactoryType() {
        return factoryType;
    }

    public void setFactoryType(String factoryType) {
        this.factoryType = factoryType;
    }

    public Double getCapacityValue() {
        return capacityValue;
    }

    public void setCapacityValue(Double capacityValue) {
        this.capacityValue = capacityValue;
    }

    public String getCapacityUnit() {
        return capacityUnit;
    }

    public void setCapacityUnit(String capacityUnit) {
        this.capacityUnit = capacityUnit;
    }

    public String getFactoryImageUrl() {
        return factoryImageUrl;
    }

    public void setFactoryImageUrl(String factoryImageUrl) {
        this.factoryImageUrl = factoryImageUrl;
    }

    public String getFactoryImages() {
        return factoryImages;
    }

    public void setFactoryImages(String factoryImages) {
        this.factoryImages = factoryImages;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getBusinessLicense() {
        return businessLicense;
    }

    public void setBusinessLicense(String businessLicense) {
        this.businessLicense = businessLicense;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getCertificationDocument() {
        return certificationDocument;
    }

    public void setCertificationDocument(String certificationDocument) {
        this.certificationDocument = certificationDocument;
    }

    public String getCertificates() {
        return certificates;
    }

    public void setCertificates(String certificates) {
        this.certificates = certificates;
    }

    public String getVerificationRejectReason() {
        return verificationRejectReason;
    }

    public void setVerificationRejectReason(String verificationRejectReason) {
        this.verificationRejectReason = verificationRejectReason;
    }

    public int getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(int completedOrders) {
        this.completedOrders = completedOrders;
    }

    public int getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(int cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public int getOnTimeOrders() {
        return onTimeOrders;
    }

    public void setOnTimeOrders(int onTimeOrders) {
        this.onTimeOrders = onTimeOrders;
    }

    public int getLateOrders() {
        return lateOrders;
    }

    public void setLateOrders(int lateOrders) {
        this.lateOrders = lateOrders;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public double getSumRatings() {
        return sumRatings;
    }

    public void setSumRatings(double sumRatings) {
        this.sumRatings = sumRatings;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
