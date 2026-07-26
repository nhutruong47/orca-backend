package org.example.backend.service;

import org.example.backend.dto.OrderContractDTO;
import org.example.backend.entity.InterGroupOrder;
import org.example.backend.entity.OrderContract;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.OrderContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderContractServiceTest {

    @Mock
    private OrderContractRepository contractRepo;
    
    @Mock
    private InterGroupOrderRepository orderRepo;
    
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderContractService contractService;

    private User buyerOwner;
    private User sellerOwner;
    private Team buyerTeam;
    private Team sellerTeam;
    private InterGroupOrder order;
    private OrderContract contract;

    @BeforeEach
    void setUp() {
        buyerOwner = new User();
        buyerOwner.setId(UUID.randomUUID());
        buyerOwner.setFullName("Buyer Owner");

        sellerOwner = new User();
        sellerOwner.setId(UUID.randomUUID());
        sellerOwner.setFullName("Seller Owner");

        buyerTeam = new Team();
        buyerTeam.setId(UUID.randomUUID());
        buyerTeam.setName("Buyer Team");
        buyerTeam.setOwner(buyerOwner);

        sellerTeam = new Team();
        sellerTeam.setId(UUID.randomUUID());
        sellerTeam.setName("Seller Team");
        sellerTeam.setOwner(sellerOwner);

        order = new InterGroupOrder();
        order.setId(UUID.randomUUID());
        order.setTitle("Test Order");
        order.setBuyerTeam(buyerTeam);
        order.setSellerTeam(sellerTeam);
        order.setStatus("CONFIRMED");

        contract = new OrderContract();
        contract.setId(UUID.randomUUID());
        contract.setOrder(order);
        contract.setStatus("DRAFT");
    }

    @Test
    void createContract_Success() {
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(contractRepo.findByOrderId(order.getId())).thenReturn(Optional.empty());
        when(contractRepo.save(any(OrderContract.class))).thenAnswer(i -> {
            OrderContract c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        OrderContractDTO dto = contractService.createContract(order.getId(), buyerOwner);
        
        assertNotNull(dto);
        assertEquals("DRAFT", dto.getStatus());
        assertTrue(dto.getTerms().contains("Buyer Team"));
        assertTrue(dto.getTerms().contains("Seller Team"));
    }

    @Test
    void createContract_AlreadyExists() {
        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));
        when(contractRepo.findByOrderId(order.getId())).thenReturn(Optional.of(contract));

        OrderContractDTO dto = contractService.createContract(order.getId(), buyerOwner);
        
        assertEquals(contract.getId(), dto.getId());
        verify(contractRepo, never()).save(any());
    }

    @Test
    void createContract_Unauthorized() {
        User randomUser = new User();
        randomUser.setId(UUID.randomUUID());

        when(orderRepo.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(RuntimeException.class, () -> contractService.createContract(order.getId(), randomUser));
    }

    @Test
    void signContract_BuyerFirst() {
        when(contractRepo.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepo.save(any())).thenReturn(contract);

        OrderContractDTO dto = contractService.signContract(contract.getId(), "http://sig.buyer", buyerOwner);

        assertEquals("http://sig.buyer", contract.getBuyerSignatureUrl());
        assertNull(contract.getSellerSignatureUrl());
        assertEquals("DRAFT", contract.getStatus());
    }

    @Test
    void signContract_BothSigned() {
        contract.setBuyerSignatureUrl("http://sig.buyer"); // Buyer already signed
        
        when(contractRepo.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepo.save(any())).thenReturn(contract);

        OrderContractDTO dto = contractService.signContract(contract.getId(), "http://sig.seller", sellerOwner);

        assertEquals("http://sig.seller", contract.getSellerSignatureUrl());
        assertEquals("SIGNED", contract.getStatus());
        assertNotNull(contract.getSignedAt());
        verify(notificationService, atLeastOnce()).createAndSend(any(User.class), any(), any(), any(), any());
    }

    @Test
    void signContract_AlreadySigned() {
        contract.setStatus("SIGNED");

        when(contractRepo.findById(contract.getId())).thenReturn(Optional.of(contract));

        assertThrows(RuntimeException.class, () -> contractService.signContract(contract.getId(), "http://sig", buyerOwner));
    }
}
