package com.cinema.management.dto;

import com.cinema.management.model.ProgramState;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgramStateChangeDTO {

    @NotNull
    private ProgramState targetState;
}
