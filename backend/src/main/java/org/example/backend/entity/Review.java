package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private InterGroupOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_team_id")
    private Team buyerTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_user_id")
    private User buyerUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_team_id", nullable = false)
    private Team sellerTeam;

    /** Overall 1-5 stars (kept for backward compat, = overallRating) */
    @Column(nullable = false)
    private int rating;

    /** Chất lượng sản phẩm 1-5 */
    @Column(name = "product_quality")
    private Integer productQuality;

    /** Tiến độ giao hàng 1-5 */
    @Column(name = "delivery_schedule")
    private Integer deliverySchedule;

    /** Hỗ trợ khách hàng 1-5 */
    @Column(name = "customer_support")
    private Integer customerSupport;

    /** Đánh giá tổng thể 1-5 */
    @Column(name = "overall_rating")
    private Integer overallRating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** Comma-separated image URLs */
    @Column(columnDefinition = "TEXT")
    private String images;

    /** ON_TIME, LATE, NOT_DELIVERED */
    @Column(name = "delivery_result", length = 20, nullable = false)
    private String deliveryResult;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Compute overall rating as average if individual ratings provided
        if (this.overallRating != null) {
            this.rating = this.overallRating;
        }
    }

    public Review() {}

    // Getters & Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public InterGroupOrder getOrder() { return order; }
    public void setOrder(InterGroupOrder order) { this.order = order; }

    public Team getBuyerTeam() { return buyerTeam; }
    public void setBuyerTeam(Team buyerTeam) { this.buyerTeam = buyerTeam; }

    public User getBuyerUser() { return buyerUser; }
    public void setBuyerUser(User buyerUser) { this.buyerUser = buyerUser; }

    public Team getSellerTeam() { return sellerTeam; }
    public void setSellerTeam(Team sellerTeam) { this.sellerTeam = sellerTeam; }

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
}
