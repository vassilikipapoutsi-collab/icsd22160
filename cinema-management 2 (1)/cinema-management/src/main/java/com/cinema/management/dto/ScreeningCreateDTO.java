package com.cinema.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreeningCreateDTO {

    @NotNull
    private Long programId;

    private String filmTitle;
    private String filmCast;
    private String filmGenres;
    private Integer filmDurationMinutes;
    private String auditoriumName;
}
