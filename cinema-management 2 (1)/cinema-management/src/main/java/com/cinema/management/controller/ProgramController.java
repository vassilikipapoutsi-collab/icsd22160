package com.cinema.management.controller;

import com.cinema.management.dto.ProgramCreateDTO;
import com.cinema.management.dto.ProgramResponseDTO;
import com.cinema.management.dto.ProgramStateChangeDTO;
import com.cinema.management.dto.ProgramUpdateDTO;
import com.cinema.management.dto.RoleAssignmentDTO;
import com.cinema.management.service.ProgramService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @PostMapping
    public ResponseEntity<ProgramResponseDTO> create(
            @Valid @RequestBody ProgramCreateDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return new ResponseEntity<>(programService.createProgram(dto, currentUsername), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ProgramUpdateDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(programService.updateProgram(id, dto, currentUsername));
    }

    @PostMapping("/{id}/programmers")
    public ResponseEntity<ProgramResponseDTO> addProgrammer(
            @PathVariable Long id,
            @Valid @RequestBody RoleAssignmentDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(programService.addProgrammer(id, dto.getUsername(), currentUsername));
    }

    @PostMapping("/{id}/staff")
    public ResponseEntity<ProgramResponseDTO> addStaff(
            @PathVariable Long id,
            @Valid @RequestBody RoleAssignmentDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(programService.addStaff(id, dto.getUsername(), currentUsername));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        programService.deleteProgram(id, currentUsername);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<ProgramResponseDTO> changeState(
            @PathVariable Long id,
            @Valid @RequestBody ProgramStateChangeDTO dto,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(programService.changeState(id, dto.getTargetState(), currentUsername));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponseDTO> view(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(programService.view(id, currentUsername));
    }

    @GetMapping
    public ResponseEntity<List<ProgramResponseDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String filmTitle,
            @RequestParam(required = false) String auditoriumName,
            @RequestHeader(value = "X-Username", required = false) String currentUsername) {
        return ResponseEntity.ok(
                programService.search(name, description, fromDate, toDate, filmTitle, auditoriumName, currentUsername));
    }
}
