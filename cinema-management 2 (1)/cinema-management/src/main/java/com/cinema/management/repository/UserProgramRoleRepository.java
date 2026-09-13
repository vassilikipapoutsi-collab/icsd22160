package com.cinema.management.repository;

import com.cinema.management.model.Program;
import com.cinema.management.model.ProgramRoleType;
import com.cinema.management.model.User;
import com.cinema.management.model.UserProgramRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProgramRoleRepository extends JpaRepository<UserProgramRole, Long> {

    Optional<UserProgramRole> findByUserAndProgram(User user, Program program);

    List<UserProgramRole> findByProgramAndRole(Program program, ProgramRoleType role);

    List<UserProgramRole> findByUser(User user);

    boolean existsByUserAndProgramAndRole(User user, Program program, ProgramRoleType role);
}
