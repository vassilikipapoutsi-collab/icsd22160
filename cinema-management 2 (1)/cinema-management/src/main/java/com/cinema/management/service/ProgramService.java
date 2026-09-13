package com.cinema.management.service;

import com.cinema.management.dto.ProgramCreateDTO;
import com.cinema.management.dto.ProgramResponseDTO;
import com.cinema.management.dto.ProgramUpdateDTO;
import com.cinema.management.exception.ConflictException;
import com.cinema.management.exception.InvalidStateTransitionException;
import com.cinema.management.exception.ResourceNotFoundException;
import com.cinema.management.exception.UnauthorizedActionException;
import com.cinema.management.model.Program;
import com.cinema.management.model.ProgramRoleType;
import com.cinema.management.model.ProgramState;
import com.cinema.management.model.Screening;
import com.cinema.management.model.ScreeningState;
import com.cinema.management.model.User;
import com.cinema.management.model.UserProgramRole;
import com.cinema.management.repository.ProgramRepository;
import com.cinema.management.repository.ScreeningRepository;
import com.cinema.management.repository.UserProgramRoleRepository;
import com.cinema.management.repository.UserRepository;
import com.cinema.management.specification.ProgramSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProgramService {

    private static final Map<ProgramState, ProgramState> ALLOWED_TRANSITIONS = Map.of(
            ProgramState.CREATED, ProgramState.SUBMISSION,
            ProgramState.SUBMISSION, ProgramState.ASSIGNMENT,
            ProgramState.ASSIGNMENT, ProgramState.REVIEW,
            ProgramState.REVIEW, ProgramState.SCHEDULING,
            ProgramState.SCHEDULING, ProgramState.FINAL_SUBMISSION,
            ProgramState.FINAL_SUBMISSION, ProgramState.DECISION,
            ProgramState.DECISION, ProgramState.ANNOUNCED
    );

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final UserProgramRoleRepository userProgramRoleRepository;
    private final ScreeningRepository screeningRepository;

    public ProgramService(ProgramRepository programRepository,
                           UserRepository userRepository,
                           UserProgramRoleRepository userProgramRoleRepository,
                           ScreeningRepository screeningRepository) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.userProgramRoleRepository = userProgramRoleRepository;
        this.screeningRepository = screeningRepository;
    }

    @Transactional
    public ProgramResponseDTO createProgram(ProgramCreateDTO dto, String currentUsername) {
        User creator = requireAuthenticated(currentUsername);
        if (programRepository.existsByName(dto.getName())) {
            throw new ConflictException("Program name already exists, το όνομα προγράμματος χρησιμοποιείται ήδη.");
        }

        Program program = new Program();
        program.setName(dto.getName());
        program.setDescription(dto.getDescription());
        program.setStartDate(dto.getStartDate());
        program.setEndDate(dto.getEndDate());
        program.setCreationDate(LocalDateTime.now());
        program.setState(ProgramState.CREATED);
        Program saved = programRepository.save(program);

        UserProgramRole role = new UserProgramRole(creator, saved, ProgramRoleType.PROGRAMMER);
        userProgramRoleRepository.save(role);
        saved.getUserRoles().add(role);

        return toResponseDTO(saved, currentUsername);
    }

    @Transactional
    public ProgramResponseDTO updateProgram(Long programId, ProgramUpdateDTO dto, String currentUsername) {
        Program program = getProgramOrThrow(programId);
        requireProgrammer(program, currentUsername);

        if (isAtOrAfter(program.getState(), ProgramState.ANNOUNCED)) {
            throw new InvalidStateTransitionException(
                    "Program updates are only allowed before ANNOUNCED, οι ενημερώσεις επιτρέπονται μόνο πριν το ANNOUNCED.");
        }
        if (dto.getName() != null && !dto.getName().equals(program.getName())) {
            if (programRepository.existsByName(dto.getName())) {
                throw new ConflictException("Program name already exists, το όνομα προγράμματος χρησιμοποιείται ήδη.");
            }
            program.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            program.setDescription(dto.getDescription());
        }
        if (dto.getStartDate() != null) {
            program.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            program.setEndDate(dto.getEndDate());
        }
        return toResponseDTO(program, currentUsername);
    }

    @Transactional
    public ProgramResponseDTO addProgrammer(Long programId, String usernameToAdd, String currentUsername) {
        Program program = getProgramOrThrow(programId);
        requireProgrammer(program, currentUsername);
        User user = getUserOrThrow(usernameToAdd);

        userProgramRoleRepository.findByUserAndProgram(user, program).ifPresent(existing -> {
            throw new ConflictException(usernameToAdd + " already has a role in this program, έχει ήδη ρόλο σε αυτό το πρόγραμμα.");
        });

        UserProgramRole role = new UserProgramRole(user, program, ProgramRoleType.PROGRAMMER);
        userProgramRoleRepository.save(role);
        program.getUserRoles().add(role);
        return toResponseDTO(program, currentUsername);
    }

    @Transactional
    public ProgramResponseDTO addStaff(Long programId, String usernameToAdd, String currentUsername) {
        Program program = getProgramOrThrow(programId);
        requireProgrammer(program, currentUsername);

        if (isAtOrAfter(program.getState(), ProgramState.SUBMISSION) && program.getState() != ProgramState.CREATED) {
            throw new InvalidStateTransitionException(
                    "STAFF set is frozen after SUBMISSION, το σύνολο STAFF είναι παγωμένο μετά το SUBMISSION.");
        }
        User user = getUserOrThrow(usernameToAdd);

        userProgramRoleRepository.findByUserAndProgram(user, program).ifPresent(existing -> {
            throw new ConflictException(usernameToAdd + " already has a role in this program, έχει ήδη ρόλο σε αυτό το πρόγραμμα.");
        });

        UserProgramRole role = new UserProgramRole(user, program, ProgramRoleType.STAFF);
        userProgramRoleRepository.save(role);
        program.getUserRoles().add(role);
        return toResponseDTO(program, currentUsername);
    }

    @Transactional
    public void deleteProgram(Long programId, String currentUsername) {
        Program program = getProgramOrThrow(programId);
        requireProgrammer(program, currentUsername);
        if (program.getState() != ProgramState.CREATED) {
            throw new InvalidStateTransitionException(
                    "Program can only be deleted while in CREATED state, διαγράφεται μόνο στην κατάσταση CREATED.");
        }
        programRepository.delete(program);
    }

    @Transactional
    public ProgramResponseDTO changeState(Long programId, ProgramState targetState, String currentUsername) {
        Program program = getProgramOrThrow(programId);
        requireProgrammer(program, currentUsername);

        ProgramState expectedNext = ALLOWED_TRANSITIONS.get(program.getState());
        if (expectedNext == null || expectedNext != targetState) {
            throw new InvalidStateTransitionException(
                    "Invalid transition from " + program.getState() + " to " + targetState
                            + ", μη επιτρεπτή μετάβαση κατάστασης.");
        }

        if (targetState == ProgramState.DECISION) {
            autoRejectUnfinishedApprovedScreenings(program);
        }

        program.setState(targetState);
        return toResponseDTO(program, currentUsername);
    }

    private void autoRejectUnfinishedApprovedScreenings(Program program) {
        List<Screening> approvedNotFinal = screeningRepository.findByProgram(program).stream()
                .filter(s -> s.getState() == ScreeningState.APPROVED && !Boolean.TRUE.equals(s.getFinallySubmitted()))
                .collect(Collectors.toList());
        for (Screening s : approvedNotFinal) {
            s.setState(ScreeningState.REJECTED);
            s.setRejectionReason("Automatically rejected, δεν υποβλήθηκε τελική εκδοχή έως τη λήψη απόφασης.");
        }
    }

    public List<ProgramResponseDTO> search(String name, String description, LocalDate fromDate, LocalDate toDate,
                                            String filmTitle, String auditoriumName, String currentUsername) {
        Specification<Program> spec = Specification.where(ProgramSpecifications.nameContains(name))
                .and(ProgramSpecifications.descriptionContains(description))
                .and(ProgramSpecifications.startsAfterOrOn(fromDate))
                .and(ProgramSpecifications.endsBeforeOrOn(toDate))
                .and(ProgramSpecifications.hasFilmTitle(filmTitle))
                .and(ProgramSpecifications.hasAuditorium(auditoriumName));

        List<Program> results = programRepository.findAll(spec);

        return results.stream()
                .filter(p -> canView(p, currentUsername))
                .sorted(Comparator.comparing(Program::getStartDate).thenComparing(Program::getName))
                .map(p -> toResponseDTO(p, currentUsername))
                .collect(Collectors.toList());
    }

    public ProgramResponseDTO view(Long programId, String currentUsername) {
        Program program = getProgramOrThrow(programId);
        if (!canView(program, currentUsername)) {
            throw new UnauthorizedActionException("Not allowed to view this program, δεν επιτρέπεται η προβολή.");
        }
        return toResponseDTO(program, currentUsername);
    }

    private boolean canView(Program program, String currentUsername) {
        if (program.getState() == ProgramState.ANNOUNCED) {
            return true;
        }
        if (currentUsername == null) {
            return false;
        }
        return userRepository.findByUsername(currentUsername)
                .flatMap(u -> userProgramRoleRepository.findByUserAndProgram(u, program))
                .isPresent();
    }

    private boolean isMember(Program program, String currentUsername) {
        if (currentUsername == null) {
            return false;
        }
        return userRepository.findByUsername(currentUsername)
                .flatMap(u -> userProgramRoleRepository.findByUserAndProgram(u, program))
                .isPresent();
    }

    private User requireAuthenticated(String currentUsername) {
        if (currentUsername == null) {
            throw new UnauthorizedActionException("Authentication required, απαιτείται σύνδεση χρήστη.");
        }
        return getUserOrThrow(currentUsername);
    }

    private void requireProgrammer(Program program, String currentUsername) {
        User user = requireAuthenticated(currentUsername);
        boolean isProgrammer = userProgramRoleRepository.existsByUserAndProgramAndRole(user, program, ProgramRoleType.PROGRAMMER);
        if (!isProgrammer) {
            throw new UnauthorizedActionException("Only a PROGRAMMER of this program may perform this action, μόνο PROGRAMMER επιτρέπεται.");
        }
    }

    private boolean isAtOrAfter(ProgramState current, ProgramState reference) {
        List<ProgramState> order = Arrays.asList(ProgramState.values());
        return order.indexOf(current) >= order.indexOf(reference);
    }

    private Program getProgramOrThrow(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found: " + programId));
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private ProgramResponseDTO toResponseDTO(Program program, String currentUsername) {
        boolean member = isMember(program, currentUsername);

        List<String> programmerNames = program.getUserRoles().stream()
                .filter(r -> r.getRole() == ProgramRoleType.PROGRAMMER)
                .map(r -> r.getUser().getFullName())
                .collect(Collectors.toList());

        List<String> staffNames = member ? program.getUserRoles().stream()
                .filter(r -> r.getRole() == ProgramRoleType.STAFF)
                .map(r -> r.getUser().getFullName())
                .collect(Collectors.toList()) : List.of();

        return new ProgramResponseDTO(
                program.getId(),
                program.getName(),
                program.getDescription(),
                program.getStartDate(),
                program.getEndDate(),
                program.getCreationDate(),
                program.getState(),
                programmerNames,
                staffNames
        );
    }
}
