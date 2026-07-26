package org.example.backend.service;

import org.example.backend.dto.CreateDisputeRequest;
import org.example.backend.dto.OrderDisputeDTO;
import org.example.backend.dto.ResolveDisputeRequest;
import org.example.backend.dto.RespondDisputeRequest;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.OrderDispute;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.entity.enums.OrderStatus;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.OrderDisputeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private OrderDisputeRepository disputeRepo;
    
    @Mock
    private InterGroupOrderRepository orderRepo;
    
    @Mock
    private OrderStateMachine stateMachine;
    
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DisputeService disputeService;

    private User buyerOwner;
    private User sellerOwner;
    private Team buyerTeam;
    private Team sellerTeam;
    private InterGroupOrder order;
    private OrderDispute dispute;

    @BeforeEach
    void setUp() {
        buyerOwner = new User();
        buyerOwner.setId(UUID.randomUUID());
        
        sellerOwner = new User();
        sellerOwner.setId(UUID.randomUUID());

        buyerTeam = new Team();
        buyerTeam.setId(UUID.randomUUID());
        buyerTeam.setOwner(buyerOwner);

        sellerTeam = new Team();
        sellerTeam.setId(UUID.randomUUID());
        sellerTeam.setOwner(sellerOwner);

        order = new InterGroupOrder();
        order.setId(UUID.randomUUID());
        order.setTitle("Test Order");
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setStatus(OrderStatus.DELIVERED.name());

        dispute = new OrderDispute();
        dispute.setId(UUID.randomUUID());
        dispute.setOrder(order);
        dispute.setOpenedByUser(buyerOwner);
        dispute.setStatus("OPEN");
    }

    @Test
    void openDispute_Success() {
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setOrderId(order.getId());
        request.setReason("Hàng hỏng");
        request.setEvidenceUrls(List.of("http://img1"));

        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(disputeRepo.save(any())).thenAnswer(i -> {
            OrderDispute d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        OrderDisputeDTO dto = disputeService.openDispute(request, buyerOwner);

        assertNotNull(dto);
        assertEquals("OPEN", dto.getStatus());
        assertEquals("Hàng hỏng", dto.getReason());
        assertEquals(OrderStatus.DISPUTED.name(), order.getStatus());
        verify(stateMachine).requireTransition(OrderStatus.DELIVERED, OrderStatus.DISPUTED);
        verify(notificationService).createAndSend(eq(sellerOwner), any(), any(), any(), any());
    }

    @Test
    void openDispute_InvalidStatus() {
        order.setStatus(OrderStatus.RFQ_CREATED.name());
        
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setOrderId(order.getId());

        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(RuntimeException.class, () -> disputeService.openDispute(request, buyerOwner));
    }

    @Test
    void respondDispute_Success() {
        RespondDisputeRequest request = new RespondDisputeRequest();
        request.setNote("Tôi sẽ đền");
        
        when(disputeRepo.findById(dispute.getId())).thenReturn(Optional.of(dispute));
        when(disputeRepo.save(any())).thenReturn(dispute);

        OrderDisputeDTO dto = disputeService.respondDispute(dispute.getId(), request, sellerOwner);

        assertEquals("RESPONDED", dto.getStatus());
        assertTrue(dispute.getResolutionNote().contains("Tôi sẽ đền"));
        verify(notificationService).createAndSend(eq(buyerOwner), any(), any(), any(), any());
    }

    @Test
    void respondDispute_Unauthorized() {
        RespondDisputeRequest request = new RespondDisputeRequest();
        
        when(disputeRepo.findById(dispute.getId())).thenReturn(Optional.of(dispute));

        assertThrows(RuntimeException.class, () -> disputeService.respondDispute(dispute.getId(), request, buyerOwner));
    }

    @Test
    void resolveDispute_Success() {
        dispute.setStatus("RESPONDED");
        order.setStatus(OrderStatus.DISPUTED.name());
        
        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setResolutionNote("Đồng ý đền bù");

        when(disputeRepo.findById(dispute.getId())).thenReturn(Optional.of(dispute));
        when(disputeRepo.save(any())).thenReturn(dispute);

        OrderDisputeDTO dto = disputeService.resolveDispute(dispute.getId(), request, sellerOwner);

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals(OrderStatus.RESOLVED.name(), order.getStatus());
        verify(stateMachine).requireTransition(OrderStatus.DISPUTED, OrderStatus.RESOLVED);
    }
}
