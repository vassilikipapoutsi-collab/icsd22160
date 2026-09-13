package com.cinema.management.controller;

import com.cinema.management.dto.ApprovalDTO;
import com.cinema.management.dto.HandlerAssignmentDTO;
import com.cinema.management.dto.RejectionDTO;
import com.cinema.management.dto.ScreeningCreateDTO;
import com.cinema.management.dto.ScreeningResponseDTO;
import com.cinema.management.dto.ScreeningReviewDTO;
import com.cinema.management.dto.ScreeningUpdateDTO;
import com.cinema.management.service.ScreeningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/screenings")
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @PostMapping
    public ResponseEntity<ScreeningResponseDTO> create(
            @Valid @RequestBody ScreeningCreateDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return new ResponseEntity<>(screeningService.create(dto, currentUsername), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScreeningResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ScreeningUpdateDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.update(id, dto, currentUsername));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdraw(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        screeningService.withdraw(id, currentUsername);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ScreeningResponseDTO> submit(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.submit(id, currentUsername));
    }

    @PostMapping("/{id}/handler")
    public ResponseEntity<ScreeningResponseDTO> assignHandler(
            @PathVariable Long id,
            @Valid @RequestBody HandlerAssignmentDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.assignHandler(id, dto.getStaffUsername(), currentUsername));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ScreeningResponseDTO> review(
            @PathVariable Long id,
            @Valid @RequestBody ScreeningReviewDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.review(id, dto, currentUsername));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ScreeningResponseDTO> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.approve(id, dto, currentUsername));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ScreeningResponseDTO> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectionDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.rejectManual(id, dto, currentUsername));
    }

    @PostMapping("/{id}/final-submit")
    public ResponseEntity<ScreeningResponseDTO> finalSubmit(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.finalSubmit(id, currentUsername));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ScreeningResponseDTO> accept(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.accept(id, currentUsername));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreeningResponseDTO> view(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(screeningService.view(id, currentUsername));
    }

    @GetMapping
    public ResponseEntity<List<ScreeningResponseDTO>> search(
            @RequestParam Long programId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String cast,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "false") boolean timetableView,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(
                screeningService.search(programId, title, cast, genre, from, to, timetableView, currentUsername));
    }
}
