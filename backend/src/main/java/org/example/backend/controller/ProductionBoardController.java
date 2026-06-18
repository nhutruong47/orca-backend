package org.example.backend.controller;

import org.example.backend.dto.DailyBoardDTO;
import org.example.backend.service.ProductionBoardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/production/board")
@CrossOrigin("*")
public class ProductionBoardController {

    private final ProductionBoardService boardService;

    public ProductionBoardController(ProductionBoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/{teamId}/today")
    public ResponseEntity<?> getTodayBoard(@PathVariable UUID teamId) {
        return ResponseEntity.ok(boardService.getDailyBoard(teamId, LocalDate.now()));
    }

    @GetMapping("/{teamId}/date/{date}")
    public ResponseEntity<?> getBoardByDate(
            @PathVariable UUID teamId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(boardService.getDailyBoard(teamId, date));
    }

    @GetMapping("/{teamId}/calendar")
    public ResponseEntity<?> getCalendarBoard(
            @PathVariable UUID teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(boardService.getCalendarBoard(teamId, startDate, endDate));
    }

    @GetMapping("/{teamId}/workforce")
    public ResponseEntity<?> getWorkforceToday(@PathVariable UUID teamId) {
        return ResponseEntity.ok(boardService.getWorkforceToday(teamId));
    }
}
