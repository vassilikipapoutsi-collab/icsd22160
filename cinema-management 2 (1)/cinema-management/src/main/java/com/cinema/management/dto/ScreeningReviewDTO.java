package com.cinema.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreeningReviewDTO {

    @NotNull
    private Integer score;

    private String comments;
}
