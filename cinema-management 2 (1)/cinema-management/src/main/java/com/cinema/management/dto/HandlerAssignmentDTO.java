package com.cinema.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandlerAssignmentDTO {

    @NotBlank
    private String staffUsername;
}
