package org.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeliveryProofImageDTO {

    private String imageUrl;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalDateTime capturedAt;

    private String imageType;

    public DeliveryProofImageDTO() {
    }

    public DeliveryProofImageDTO(String imageUrl, BigDecimal latitude, BigDecimal longitude,
                                 LocalDateTime capturedAt, String imageType) {
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capturedAt = capturedAt;
        this.imageType = imageType;
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }

    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }
}