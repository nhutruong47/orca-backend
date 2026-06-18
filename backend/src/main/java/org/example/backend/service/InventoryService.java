package org.example.backend.service;

import org.example.backend.dto.InventoryItemDTO;
import org.example.backend.entity.InventoryItem;
import org.example.backend.entity.Team;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepo;
    private final TeamRepository teamRepo;

    public InventoryService(InventoryRepository inventoryRepo, TeamRepository teamRepo) {
        this.inventoryRepo = inventoryRepo;
        this.teamRepo = teamRepo;
    }

    // ========== READ ==========

    public List<InventoryItemDTO> getByTeam(UUID teamId) {
        return inventoryRepo.findByTeamIdOrderByProductTypeAscProductStateAsc(teamId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Get inventory grouped by product type for dashboard display */
    public Map<String, Map<String, Double>> getInventoryMatrix(UUID teamId) {
        List<InventoryItem> items = inventoryRepo.findByTeamIdOrderByProductTypeAscProductStateAsc(teamId);
        Map<String, Map<String, Double>> matrix = new LinkedHashMap<>();
        for (InventoryItem item : items) {
            matrix.computeIfAbsent(item.getProductType(), k -> new LinkedHashMap<>())
                    .put(item.getProductState(), item.getQuantity());
        }
        return matrix;
    }

    // ========== CREATE ==========

    public InventoryItemDTO create(InventoryItemDTO dto) {
        Team t = teamRepo.findById(UUID.fromString(dto.getTeamId()))
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Check if already exists
        Optional<InventoryItem> existing = inventoryRepo.findByTeamIdAndProductTypeAndProductState(
                t.getId(), dto.getProductType(), dto.getProductState());
        if (existing.isPresent()) {
            throw new RuntimeException("Mục kho '" + dto.getProductType() + " - " + dto.getProductState() + "' đã tồn tại.");
        }

        InventoryItem item = new InventoryItem();
        item.setTeam(t);
        item.setProductType(dto.getProductType());
        item.setProductState(dto.getProductState() != null ? dto.getProductState() : "GREEN");
        item.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0.0);
        item.setUnit(dto.getUnit() != null ? dto.getUnit() : "kg");
        item.setLowStockThreshold(dto.getLowStockThreshold() != null ? dto.getLowStockThreshold() : 100.0);

        return toDTO(inventoryRepo.save(item));
    }

    // ========== UPDATE ==========

    public InventoryItemDTO updateQuantity(UUID id, Double newQuantity) {
        InventoryItem item = inventoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        item.setQuantity(newQuantity);
        return toDTO(inventoryRepo.save(item));
    }

    // ========== DELETE ==========

    public void delete(UUID id) {
        inventoryRepo.deleteById(id);
    }

    // ========== AUTO INVENTORY UPDATE (Production Workflow) ==========

    /**
     * Automatically adjust inventory when a production stage completes.
     * @param teamId       the team/factory
     * @param productType  e.g. "Arabica", "Robusta"
     * @param fromState    source state (e.g. "GREEN")
     * @param toState      target state (e.g. "ROASTED")
     * @param quantity     amount to transfer
     */
    @Transactional
    public void transferStock(UUID teamId, String productType, String fromState, String toState, double quantity) {
        if (quantity <= 0) return;

        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Deduct from source
        InventoryItem source = getOrCreate(team, productType, fromState);
        double newSourceQty = Math.max(0, source.getQuantity() - quantity);
        source.setQuantity(newSourceQty);
        inventoryRepo.save(source);

        // Add to target
        InventoryItem target = getOrCreate(team, productType, toState);
        target.setQuantity(target.getQuantity() + quantity);
        inventoryRepo.save(target);
    }

    /**
     * Deduct from packaged stock when order is delivered.
     */
    @Transactional
    public void deductPackagedStock(UUID teamId, String productType, double quantity) {
        if (quantity <= 0) return;

        InventoryItem item = inventoryRepo.findByTeamIdAndProductTypeAndProductState(teamId, productType, "PACKAGED")
                .orElse(null);
        if (item != null) {
            item.setQuantity(Math.max(0, item.getQuantity() - quantity));
            inventoryRepo.save(item);
        }
    }

    /**
     * Initialize default inventory items for a team (4 types x 4 states).
     */
    @Transactional
    public void initializeDefaultInventory(UUID teamId) {
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        String[] types = {"Arabica", "Robusta", "Culi", "Blend"};
        String[] states = {"GREEN", "ROASTED", "GROUND", "PACKAGED"};
        for (String type : types) {
            for (String state : states) {
                Optional<InventoryItem> existing = inventoryRepo.findByTeamIdAndProductTypeAndProductState(teamId, type, state);
                if (existing.isEmpty()) {
                    InventoryItem item = new InventoryItem();
                    item.setTeam(team);
                    item.setProductType(type);
                    item.setProductState(state);
                    item.setQuantity(0.0);
                    item.setUnit("kg");
                    inventoryRepo.save(item);
                }
            }
        }
    }

    // ========== HELPERS ==========

    private InventoryItem getOrCreate(Team team, String productType, String productState) {
        return inventoryRepo.findByTeamIdAndProductTypeAndProductState(team.getId(), productType, productState)
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setTeam(team);
                    newItem.setProductType(productType);
                    newItem.setProductState(productState);
                    newItem.setQuantity(0.0);
                    newItem.setUnit("kg");
                    return inventoryRepo.save(newItem);
                });
    }

    public InventoryItemDTO toDTO(InventoryItem i) {
        InventoryItemDTO dto = new InventoryItemDTO();
        dto.setId(i.getId().toString());
        dto.setTeamId(i.getTeam().getId().toString());
        dto.setProductType(i.getProductType());
        dto.setProductState(i.getProductState());
        dto.setDisplayName(i.getDisplayName());
        dto.setName(i.getDisplayName());
        dto.setQuantity(i.getQuantity());
        dto.setUnit(i.getUnit());
        dto.setLowStockThreshold(i.getLowStockThreshold());
        dto.setStatus(i.getStockStatus());
        dto.setLastUpdated(i.getLastUpdated());
        return dto;
    }
}
