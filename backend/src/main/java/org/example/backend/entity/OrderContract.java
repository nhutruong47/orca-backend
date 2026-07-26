package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderContract {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private InterGroupOrder order;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String terms;

    @Column(name = "buyer_signature_url", columnDefinition = "TEXT")
    private String buyerSignatureUrl;

    @Column(name = "seller_signature_url", columnDefinition = "TEXT")
    private String sellerSignatureUrl;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(length = 20)
    private String status = "DRAFT"; // DRAFT, SIGNED, CANCELED

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
