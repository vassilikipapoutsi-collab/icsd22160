package com.cinema.management.specification;

import com.cinema.management.model.Program;
import com.cinema.management.model.Screening;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ProgramSpecifications {

    private ProgramSpecifications() {
    }

    public static Specification<Program> nameContains(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank()) ? null
                        : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Program> descriptionContains(String description) {
        return (root, query, cb) ->
                (description == null || description.isBlank()) ? null
                        : cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
    }

    public static Specification<Program> startsAfterOrOn(LocalDate date) {
        return (root, query, cb) ->
                date == null ? null : cb.greaterThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<Program> endsBeforeOrOn(LocalDate date) {
        return (root, query, cb) ->
                date == null ? null : cb.lessThanOrEqualTo(root.get("endDate"), date);
    }

    public static Specification<Program> hasFilmTitle(String filmTitle) {
        return (root, query, cb) -> {
            if (filmTitle == null || filmTitle.isBlank()) {
                return null;
            }
            query.distinct(true);
            var screeningJoin = root.<Program, Screening>join("screenings");
            return cb.like(cb.lower(screeningJoin.get("filmTitle")), "%" + filmTitle.toLowerCase() + "%");
        };
    }

    public static Specification<Program> hasAuditorium(String auditoriumName) {
        return (root, query, cb) -> {
            if (auditoriumName == null || auditoriumName.isBlank()) {
                return null;
            }
            query.distinct(true);
            var screeningJoin = root.<Program, Screening>join("screenings");
            return cb.like(cb.lower(screeningJoin.get("auditoriumName")), "%" + auditoriumName.toLowerCase() + "%");
        };
    }
}
