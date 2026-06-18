package org.example.backend.dto;

import java.time.LocalDateTime;

public class ReviewDTO {
    private String id;
    private String orderId;
    private String buyerTeamId;
    private String buyerUserId;
    private String sellerTeamId;
    private int rating;
    private Integer productQuality;
    private Integer deliverySchedule;
    private Integer customerSupport;
    private Integer overallRating;
    private String comment;
    private String images;
    private String deliveryResult;
    private LocalDateTime createdAt;
    private String buyerTeamName;
    private String buyerUserName;

    public ReviewDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getBuyerTeamId() { return buyerTeamId; }
    public void setBuyerTeamId(String buyerTeamId) { this.buyerTeamId = buyerTeamId; }

    public String getBuyerUserId() { return buyerUserId; }
    public void setBuyerUserId(String buyerUserId) { this.buyerUserId = buyerUserId; }

    public String getSellerTeamId() { return sellerTeamId; }
    public void setSellerTeamId(String sellerTeamId) { this.sellerTeamId = sellerTeamId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public Integer getProductQuality() { return productQuality; }
    public void setProductQuality(Integer productQuality) { this.productQuality = productQuality; }

    public Integer getDeliverySchedule() { return deliverySchedule; }
    public void setDeliverySchedule(Integer deliverySchedule) { this.deliverySchedule = deliverySchedule; }

    public Integer getCustomerSupport() { return customerSupport; }
    public void setCustomerSupport(Integer customerSupport) { this.customerSupport = customerSupport; }

    public Integer getOverallRating() { return overallRating; }
    public void setOverallRating(Integer overallRating) { this.overallRating = overallRating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getDeliveryResult() { return deliveryResult; }
    public void setDeliveryResult(String deliveryResult) { this.deliveryResult = deliveryResult; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getBuyerTeamName() { return buyerTeamName; }
    public void setBuyerTeamName(String buyerTeamName) { this.buyerTeamName = buyerTeamName; }

    public String getBuyerUserName() { return buyerUserName; }
    public void setBuyerUserName(String buyerUserName) { this.buyerUserName = buyerUserName; }
}
