package com.cinema.management.dto;

import com.cinema.management.model.ScreeningState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningResponseDTO {

    private Long id;
    private Long programId;
    private ScreeningState state;
    private String filmTitle;
    private String filmCast;
    private String filmGenres;
    private Integer filmDurationMinutes;
    private String auditoriumName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String submitterUsername;
    private String handlerUsername;
    private Integer reviewScore;
    private String reviewComments;
    private String rejectionReason;
}
