package org.example.backend.config;

import org.example.backend.entity.Team;
import org.example.backend.repository.TeamRepository;
import org.example.backend.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@DependsOn("mockDataInitializer")
public class InventoryBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryBackfillRunner.class);

    private final TeamRepository teamRepository;
    private final InventoryService inventoryService;

    public InventoryBackfillRunner(TeamRepository teamRepository, InventoryService inventoryService) {
        this.teamRepository = teamRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(String... args) {
        log.info("Checking and backfilling default inventory for all teams...");
        List<Team> teams = teamRepository.findAll();
        int backfilledCount = 0;
        
        for (Team team : teams) {
            try {
                inventoryService.initializeDefaultInventory(team.getId());
                backfilledCount++;
            } catch (Exception e) {
                log.error("Error backfilling inventory for team: {}", team.getId(), e);
            }
        }
        
        log.info("Finished inventory backfill check. Processed {} teams.", backfilledCount);
    }
}
