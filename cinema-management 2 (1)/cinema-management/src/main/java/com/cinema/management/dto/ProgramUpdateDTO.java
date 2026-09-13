package com.cinema.management.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProgramUpdateDTO {

    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}
