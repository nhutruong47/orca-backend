package org.example.backend.repository;

import org.example.backend.entity.InterGroupOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InterGroupOrderRepository extends JpaRepository<InterGroupOrder, UUID> {

    // Đơn mình đi đặt (Xưởng của mình là Buyer)
    List<InterGroupOrder> findByBuyerTeamIdOrderByCreatedAtDesc(UUID buyerTeamId);

    // Đơn xưởng khác đặt mình (Xưởng của mình là Seller)
    List<InterGroupOrder> findBySellerTeamIdOrderByCreatedAtDesc(UUID sellerTeamId);
}
