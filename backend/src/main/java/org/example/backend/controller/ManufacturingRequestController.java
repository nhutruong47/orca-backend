package org.example.backend.controller;

import org.example.backend.dto.ManufacturingRequestDTO;
import org.example.backend.entity.User;
import org.example.backend.service.ManufacturingRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manufacturing-requests")
public class ManufacturingRequestController {

    private final ManufacturingRequestService service;

    public ManufacturingRequestController(ManufacturingRequestService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ManufacturingRequestDTO>> getAll() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @PostMapping
    public ResponseEntity<ManufacturingRequestDTO> create(@RequestBody ManufacturingRequestDTO dto, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.createRequest(dto, user));
    }
}
