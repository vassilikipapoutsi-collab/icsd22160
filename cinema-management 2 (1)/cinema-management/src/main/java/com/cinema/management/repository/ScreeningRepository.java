package com.cinema.management.repository;

import com.cinema.management.model.Program;
import com.cinema.management.model.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ScreeningRepository extends JpaRepository<Screening, Long>, JpaSpecificationExecutor<Screening> {

    List<Screening> findByProgram(Program program);
}
