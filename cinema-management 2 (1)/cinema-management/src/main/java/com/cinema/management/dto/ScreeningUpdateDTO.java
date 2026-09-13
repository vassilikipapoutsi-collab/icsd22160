package com.cinema.management.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScreeningUpdateDTO {

    private String filmTitle;
    private String filmCast;
    private String filmGenres;
    private Integer filmDurationMinutes;
    private String auditoriumName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
