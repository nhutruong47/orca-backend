package org.example.backend.service;

import org.example.backend.dto.InterGroupOrderDTO;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the new contract:
 *  - Users outside a team receive 403 / AccessDeniedException when touching team-scoped data.
 *  - Inter-group order lifecycle: RFQ_CREATED -> CONFIRMED -> SHIPPING -> DELIVERED -> COMPLETED.
 */
@SpringBootTest
@Transactional
public class AuthorizationAndLifecycleTest {

    @Autowired private AccessControlService accessControlService;
    @Autowired private InterGroupOrderService interGroupOrderService;
    @Autowired private InterGroupOrderRepository orderRepo;
    @Autowired private TeamRepository teamRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private TeamMemberRepository teamMemberRepo;
    @Autowired private ReviewRepository reviewRepo;

    private User seller;
    private User buyer;
    private User stranger;
    private Team sellerTeam;
    private Team buyerTeam;

    @BeforeEach
    void setUp() {
        seller = persistUser("seller_");
        buyer = persistUser("buyer_");
        stranger = persistUser("stranger_");

        sellerTeam = createTeamWithOwner(seller);
        buyerTeam = createTeamWithOwner(buyer);
    }

    private User persistUser(String prefix) {
        User u = new User();
        u.setUsername(prefix + UUID.randomUUID());
        u.setPassword("pass");
        u.setEmail(prefix + UUID.randomUUID() + "@example.com");
        return userRepo.save(u);
    }

    private Team createTeamWithOwner(User owner) {
        Team t = new Team();
        t.setName("Team of " + owner.getUsername());
        t.setOwner(owner);
        Team saved = teamRepo.save(t);

        TeamMember tm = new TeamMember();
        tm.setTeam(saved);
        tm.setUser(owner);
        tm.setGroupRole(GroupRole.ADMIN);
        teamMemberRepo.save(tm);
        return saved;
    }

    // ===== Authorization =====

    @Test
    void requireTeamMember_shouldThrow_forStranger() {
        assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireTeamMember(stranger, sellerTeam.getId()));
    }

    @Test
    void requireTeamMember_shouldPass_forMember() {
        assertDoesNotThrow(() -> accessControlService.requireTeamMember(seller, sellerTeam.getId()));
    }

    @Test
    void requireInterGroupOrderAccess_shouldThrow_forStranger() {
        InterGroupOrder order = new InterGroupOrder();
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setTitle("Test");
        order.setQuantity(10);
        order.setStatus("RFQ_CREATED");
        InterGroupOrder saved = orderRepo.save(order);

        assertThrows(ResponseStatusException.class,
                () -> accessControlService.requireInterGroupOrderAccess(stranger, saved));
    }

    @Test
    void requireInterGroupOrderAccess_shouldPass_forBuyerAndSeller() {
        InterGroupOrder order = new InterGroupOrder();
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setTitle("Test");
        order.setQuantity(10);
        order.setStatus("RFQ_CREATED");
        InterGroupOrder saved = orderRepo.save(order);

        assertDoesNotThrow(() -> accessControlService.requireInterGroupOrderAccess(seller, saved));
        assertDoesNotThrow(() -> accessControlService.requireInterGroupOrderAccess(buyer, saved));
    }

    // ===== Inter-Group Lifecycle =====

    @Test
    void interGroupLifecycle_createAcceptDeliverConfirm() {
        // 1. Buyer creates RFQ
        InterGroupOrderDTO dto = new InterGroupOrderDTO();
        dto.setSellerTeamId(sellerTeam.getId().toString());
        dto.setBuyerTeamId(buyerTeam.getId().toString());
        dto.setTitle("Lifecycle Test Order");
        dto.setDescription("desc");
        dto.setQuantity(50);
        dto.setStatus("RFQ_CREATED");

        InterGroupOrderDTO created = interGroupOrderService.createOrder(dto, buyer);
        assertEquals("RFQ_CREATED", created.getStatus());

        // 2. Seller accepts -> CONFIRMED
        InterGroupOrderDTO accepted = interGroupOrderService.acceptOrder(
                UUID.fromString(created.getId()), seller);
        assertEquals("CONFIRMED", accepted.getStatus());
        assertNotNull(accepted.getLinkedGoalId());

        // 3. Seller delivers -> DELIVERED
        InterGroupOrderDTO delivered = interGroupOrderService.deliverOrder(
                UUID.fromString(created.getId()), "Giao tai kho", seller, null);
        assertEquals("DELIVERED", delivered.getStatus());
        assertFalse(Boolean.TRUE.equals(delivered.getDeliveryConfirmed()));

        // 4. Buyer confirms -> COMPLETED
        InterGroupOrderDTO confirmed = interGroupOrderService.confirmDelivery(
                UUID.fromString(created.getId()), "ON_TIME", 5, "OK", buyer);
        assertEquals("COMPLETED", confirmed.getStatus());
        assertTrue(Boolean.TRUE.equals(confirmed.getDeliveryConfirmed()));
        assertEquals("ON_TIME", confirmed.getDeliveryStatus());

        // A review should have been saved
        long reviewCount = reviewRepo.findAll().stream()
                .filter(r -> r.getOrder() != null && r.getOrder().getId().equals(UUID.fromString(created.getId())))
                .count();
        assertTrue(reviewCount >= 1, "Review should have been recorded");
    }

    @Test
    void deliverOrder_byBuyer_shouldThrow() {
        InterGroupOrder order = new InterGroupOrder();
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setTitle("Auth Test");
        order.setQuantity(10);
        order.setStatus("CONFIRMED");
        InterGroupOrder saved = orderRepo.save(order);

        // Buyer is not the seller
        assertThrows(RuntimeException.class,
                () -> interGroupOrderService.deliverOrder(saved.getId(), null, buyer, null));
    }

    @Test
    void confirmDelivery_bySeller_shouldThrow() {
        InterGroupOrder order = new InterGroupOrder();
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setTitle("Auth Test 2");
        order.setQuantity(10);
        order.setStatus("DELIVERED");
        InterGroupOrder saved = orderRepo.save(order);

        assertThrows(RuntimeException.class,
                () -> interGroupOrderService.confirmDelivery(saved.getId(), "ON_TIME", 5, "ok", seller));
    }

    @Test
    void deliverOrder_invalidStatus_shouldThrow() {
        InterGroupOrder order = new InterGroupOrder();
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setTitle("Status Test");
        order.setQuantity(10);
        order.setStatus("REJECTED");
        InterGroupOrder saved = orderRepo.save(order);

        assertThrows(RuntimeException.class,
                () -> interGroupOrderService.deliverOrder(saved.getId(), null, seller, null));
    }

    @Test
    void confirmDelivery_invalidStatus_shouldThrow() {
        InterGroupOrder order = new InterGroupOrder();
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setTitle("Status Test 2");
        order.setQuantity(10);
        order.setStatus("CONFIRMED");
        InterGroupOrder saved = orderRepo.save(order);

        assertThrows(RuntimeException.class,
                () -> interGroupOrderService.confirmDelivery(saved.getId(), "ON_TIME", 5, "ok", buyer));
    }
}