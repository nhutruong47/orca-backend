package org.example.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ConfirmDeliveryRequest {
    @NotBlank(message = "Delivery status is required")
    @Pattern(regexp = "ON_TIME|LATE|NOT_DELIVERED",
            message = "Delivery status must be one of ON_TIME, LATE, NOT_DELIVERED")
    private String deliveryStatus;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 500, message = "Comment must be at most 500 characters")
    private String comment;

    @Size(max = 500, message = "Delivery note must be at most 500 characters")
    private String deliveryNote;

    private java.util.List<String> proofImageUrls;

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String deliveryNote) { this.deliveryNote = deliveryNote; }

    public java.util.List<String> getProofImageUrls() { return proofImageUrls; }
    public void setProofImageUrls(java.util.List<String> proofImageUrls) { this.proofImageUrls = proofImageUrls; }
}
