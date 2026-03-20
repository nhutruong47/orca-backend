package org.example.backend.service;

import org.example.backend.dto.InventoryItemDTO;
import org.example.backend.entity.InventoryItem;
import org.example.backend.entity.Team;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepo;
    private final TeamRepository teamRepo;

    public InventoryService(InventoryRepository inventoryRepo, TeamRepository teamRepo) {
        this.inventoryRepo = inventoryRepo;
        this.teamRepo = teamRepo;
    }

    public List<InventoryItemDTO> getByTeam(UUID teamId) {
        return inventoryRepo.findByTeamIdOrderByLastUpdatedDesc(teamId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public InventoryItemDTO create(InventoryItemDTO dto) {
        Team t = teamRepo.findById(UUID.fromString(dto.getTeamId()))
                .orElseThrow(() -> new RuntimeException("Team not found"));
        
        InventoryItem item = new InventoryItem();
        item.setTeam(t);
        item.setName(dto.getName());
        item.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0.0);
        item.setUnit(dto.getUnit());
        item.setLowStockThreshold(dto.getLowStockThreshold() != null ? dto.getLowStockThreshold() : 10.0);
        
        return toDTO(inventoryRepo.save(item));
    }

    public InventoryItemDTO updateQuantity(UUID id, Double newQuantity) {
        InventoryItem item = inventoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        item.setQuantity(newQuantity);
        return toDTO(inventoryRepo.save(item));
    }

    public void delete(UUID id) {
        inventoryRepo.deleteById(id);
    }

    private InventoryItemDTO toDTO(InventoryItem i) {
        InventoryItemDTO dto = new InventoryItemDTO();
        dto.setId(i.getId().toString());
        dto.setTeamId(i.getTeam().getId().toString());
        dto.setName(i.getName());
        dto.setQuantity(i.getQuantity());
        dto.setUnit(i.getUnit());
        dto.setLowStockThreshold(i.getLowStockThreshold());
        dto.setStatus(i.getStockStatus());
        dto.setLastUpdated(i.getLastUpdated());
        return dto;
    }
}
