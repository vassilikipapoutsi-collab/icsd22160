package com.cinema.management.specification;

import com.cinema.management.model.Program;
import com.cinema.management.model.Screening;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class ScreeningSpecifications {

    private ScreeningSpecifications() {
    }

    public static Specification<Screening> inProgram(Program program) {
        return (root, query, cb) -> cb.equal(root.get("program"), program);
    }

    public static Specification<Screening> titleContainsAllWords(String title) {
        return (root, query, cb) -> allWordsMatch(cb, root.get("filmTitle"), title);
    }

    public static Specification<Screening> castContainsAllWords(String cast) {
        return (root, query, cb) -> allWordsMatch(cb, root.get("filmCast"), cast);
    }

    public static Specification<Screening> genreContainsAllWords(String genre) {
        return (root, query, cb) -> allWordsMatch(cb, root.get("filmGenres"), genre);
    }

    public static Specification<Screening> startsFrom(LocalDateTime from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("startTime"), from);
    }

    public static Specification<Screening> startsUntil(LocalDateTime until) {
        return (root, query, cb) ->
                until == null ? null : cb.lessThanOrEqualTo(root.get("startTime"), until);
    }

    private static Predicate allWordsMatch(CriteriaBuilder cb, Path<String> path, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] words = value.toLowerCase().split("\\s+");
        Predicate predicate = cb.conjunction();
        for (String word : words) {
            predicate = cb.and(predicate, cb.like(cb.lower(path), "%" + word + "%"));
        }
        return predicate;
    }
}
