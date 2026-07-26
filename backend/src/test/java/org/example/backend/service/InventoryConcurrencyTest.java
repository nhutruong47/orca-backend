package org.example.backend.service;

import org.example.backend.entity.InventoryItem;
import org.example.backend.entity.Team;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for concurrent inventory deductions (F1.3).
 *
 * <p>Spins up 8 threads that each try to deduct 1 unit from the same row
 * starting at qty=10. With the optimistic-lock retry in place, the final
 * quantity must be exactly 2 (10 - 8 deductions = 2). Without the retry,
 * a race condition would let multiple writers overwrite each other and
 * leave the quantity higher than 2.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepo;

    @Autowired
    private TeamRepository teamRepo;

    @Autowired
    private org.example.backend.repository.UserRepository userRepo;

    @Test
    void concurrentDeductionsNeverLoseAnUpdate() throws InterruptedException {
        // arrange: create a packaged inventory row with qty=10
        org.example.backend.entity.User owner = new org.example.backend.entity.User();
        owner.setUsername("owner_" + UUID.randomUUID().toString().substring(0, 8));
        owner.setEmail(owner.getUsername() + "@test.com");
        owner.setPassword("pass");
        owner.setRole(org.example.backend.entity.Role.MEMBER);
        userRepo.saveAndFlush(owner);

        Team team = new Team();
        team.setName("Test Team");
        team.setOwner(owner);
        teamRepo.saveAndFlush(team);
        
        String productType = "TEST_CONCURRENT_" + UUID.randomUUID();
        InventoryItem initial = new InventoryItem();
        initial.setTeam(team);
        initial.setProductType(productType);
        initial.setProductState("PACKAGED");
        initial.setQuantity(10.0);
        initial.setUnit("kg");
        inventoryRepo.saveAndFlush(initial);

        // act: 8 threads each deduct 1
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    inventoryService.deductPackagedStock(team.getId(), productType, 1.0);
                } catch (Throwable t) {
                    t.printStackTrace();
                    failures.incrementAndGet();
                }
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Deductions did not finish in time");

        // assert: final quantity must be 10 - 8 = 2. No deduction may have been lost.
        InventoryItem finalRow = inventoryRepo
                .findByTeamIdAndProductTypeAndProductState(team.getId(), productType, "PACKAGED")
                .orElseThrow();
        assertEquals(2.0, finalRow.getQuantity(),
                "Expected final quantity 2.0 after 8 concurrent deductions of 1 each; "
                        + "value=" + finalRow.getQuantity() + " threadFailures=" + failures.get());
        // All threads should have succeeded (NO OptimisticLockingFailureException leaked to caller).
        assertEquals(0, failures.get(),
                "Optimistic-lock retry should swallow all conflicts; " + failures.get() + " thread(s) failed");
    }
}