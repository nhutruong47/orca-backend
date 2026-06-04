package org.example.backend.controller;

import org.example.backend.dto.InventoryItemDTO;
import org.example.backend.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryItemDTO>> getByTeam(@RequestParam UUID teamId) {
        return ResponseEntity.ok(inventoryService.getByTeam(teamId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody InventoryItemDTO dto) {
        try {
            return ResponseEntity.ok(inventoryService.create(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/quantity")
    public ResponseEntity<?> updateQuantity(@PathVariable UUID id, @RequestBody Map<String, Double> body) {
        try {
            Double qty = body.get("quantity");
            if (qty == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập số lượng (quantity)"));
            }
            return ResponseEntity.ok(inventoryService.updateQuantity(id, qty));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            inventoryService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa mặt hàng khỏi kho"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
