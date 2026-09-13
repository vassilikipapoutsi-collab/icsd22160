package com.cinema.management.service;

import com.cinema.management.dto.ApprovalDTO;
import com.cinema.management.dto.RejectionDTO;
import com.cinema.management.dto.ScreeningCreateDTO;
import com.cinema.management.dto.ScreeningResponseDTO;
import com.cinema.management.dto.ScreeningReviewDTO;
import com.cinema.management.dto.ScreeningUpdateDTO;
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
import com.cinema.management.specification.ScreeningSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final UserProgramRoleRepository userProgramRoleRepository;

    public ScreeningService(ScreeningRepository screeningRepository,
                             ProgramRepository programRepository,
                             UserRepository userRepository,
                             UserProgramRoleRepository userProgramRoleRepository) {
        this.screeningRepository = screeningRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.userProgramRoleRepository = userProgramRoleRepository;
    }

    @Transactional
    public ScreeningResponseDTO create(ScreeningCreateDTO dto, String currentUsername) {
        User user = requireAuthenticated(currentUsername);
        Program program = getProgramOrThrow(dto.getProgramId());

        if (program.getState() == ProgramState.ANNOUNCED) {
            throw new InvalidStateTransitionException(
                    "Program is already announced, δεν επιτρέπεται πλέον δημιουργία προβολών.");
        }

        Optional<UserProgramRole> existingRole = userProgramRoleRepository.findByUserAndProgram(user, program);
        if (existingRole.isPresent() && existingRole.get().getRole() != ProgramRoleType.SUBMITTER) {
            throw new UnauthorizedActionException(
                    "A " + existingRole.get().getRole() + " may not also submit screenings in this program, "
                            + "δεν επιτρέπεται υποβολή προβολών με αυτόν τον ρόλο.");
        }
        if (existingRole.isEmpty()) {
            userProgramRoleRepository.save(new UserProgramRole(user, program, ProgramRoleType.SUBMITTER));
        }

        Screening screening = new Screening();
        screening.setProgram(program);
        screening.setSubmitter(user);
        screening.setCreationDate(LocalDateTime.now());
        screening.setState(ScreeningState.CREATED);
        screening.setFilmTitle(dto.getFilmTitle());
        screening.setFilmCast(dto.getFilmCast());
        screening.setFilmGenres(dto.getFilmGenres());
        screening.setFilmDurationMinutes(dto.getFilmDurationMinutes());
        screening.setAuditoriumName(dto.getAuditoriumName());

        Screening saved = screeningRepository.save(screening);
        return toResponseDTO(saved, currentUsername);
    }

    @Transactional
    public ScreeningResponseDTO update(Long screeningId, ScreeningUpdateDTO dto, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        requireSubmitter(screening, currentUsername);

        if (screening.getState() != ScreeningState.CREATED) {
            throw new InvalidStateTransitionException(
                    "Only CREATED screenings can be updated, ενημερώνονται μόνο προβολές σε κατάσταση CREATED.");
        }
        if (dto.getFilmTitle() != null) {
            screening.setFilmTitle(dto.getFilmTitle());
        }
        if (dto.getFilmCast() != null) {
            screening.setFilmCast(dto.getFilmCast());
        }
        if (dto.getFilmGenres() != null) {
            screening.setFilmGenres(dto.getFilmGenres());
        }
        if (dto.getFilmDurationMinutes() != null) {
            screening.setFilmDurationMinutes(dto.getFilmDurationMinutes());
        }
        if (dto.getAuditoriumName() != null) {
            screening.setAuditoriumName(dto.getAuditoriumName());
        }
        if (dto.getStartTime() != null) {
            screening.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            screening.setEndTime(dto.getEndTime());
        }

        if (screening.getStartTime() != null && screening.getEndTime() != null
                && screening.getFilmDurationMinutes() != null) {
            long minutes = Duration.between(screening.getStartTime(), screening.getEndTime()).toMinutes();
            if (minutes < screening.getFilmDurationMinutes()) {
                throw new IllegalArgumentException(
                        "End time must be at least the film duration after start time, "
                                + "η λήξη πρέπει να απέχει τουλάχιστον όσο η διάρκεια της ταινίας.");
            }
        }
        return toResponseDTO(screening, currentUsername);
    }

    @Transactional
    public void withdraw(Long screeningId, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        requireSubmitter(screening, currentUsername);
        if (screening.getState() != ScreeningState.CREATED) {
            throw new InvalidStateTransitionException(
                    "Only CREATED screenings can be withdrawn, αποσύρονται μόνο προβολές σε κατάσταση CREATED.");
        }
        screeningRepository.delete(screening);
    }

    @Transactional
    public ScreeningResponseDTO submit(Long screeningId, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        requireSubmitter(screening, currentUsername);

        Program program = screening.getProgram();
        if (program.getState() != ProgramState.SUBMISSION) {
            throw new InvalidStateTransitionException(
                    "Program is not accepting submissions right now, το πρόγραμμα δεν δέχεται υποβολές αυτή τη στιγμή.");
        }
        if (screening.getState() != ScreeningState.CREATED) {
            throw new InvalidStateTransitionException(
                    "Only CREATED screenings can be submitted, υποβάλλονται μόνο προβολές σε κατάσταση CREATED.");
        }
        if (!isComplete(screening)) {
            throw new IllegalArgumentException(
                    "Screening is incomplete, η προβολή δεν έχει όλα τα απαραίτητα στοιχεία.");
        }
        screening.setState(ScreeningState.SUBMITTED);
        return toResponseDTO(screening, currentUsername);
    }

    private boolean isComplete(Screening screening) {
        if (screening.getFilmTitle() == null || screening.getAuditoriumName() == null
                || screening.getFilmDurationMinutes() == null || screening.getStartTime() == null
                || screening.getEndTime() == null) {
            return false;
        }
        long minutes = Duration.between(screening.getStartTime(), screening.getEndTime()).toMinutes();
        return minutes >= screening.getFilmDurationMinutes();
    }

    @Transactional
    public ScreeningResponseDTO assignHandler(Long screeningId, String staffUsername, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        Program program = screening.getProgram();
        requireProgrammer(program, currentUsername);

        if (program.getState() != ProgramState.ASSIGNMENT) {
            throw new InvalidStateTransitionException(
                    "Handlers can only be assigned during ASSIGNMENT, οι χειριστές ανατίθενται μόνο στο ASSIGNMENT.");
        }
        User staff = getUserOrThrow(staffUsername);
        boolean isStaffOfProgram = userProgramRoleRepository.existsByUserAndProgramAndRole(
                staff, program, ProgramRoleType.STAFF);
        if (!isStaffOfProgram) {
            throw new IllegalArgumentException(
                    staffUsername + " is not registered as STAFF for this program, δεν είναι STAFF σε αυτό το πρόγραμμα.");
        }
        screening.setHandler(staff);
        return toResponseDTO(screening, currentUsername);
    }

    @Transactional
    public ScreeningResponseDTO review(Long screeningId, ScreeningReviewDTO dto, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        Program program = screening.getProgram();

        if (currentUsername == null || screening.getHandler() == null
                || !screening.getHandler().getUsername().equals(currentUsername)) {
            throw new UnauthorizedActionException(
                    "Only the assigned STAFF handler may review this screening, "
                            + "μόνο ο ανατεθειμένος STAFF μπορεί να αξιολογήσει.");
        }
        if (program.getState() != ProgramState.REVIEW) {
            throw new InvalidStateTransitionException(
                    "Reviews are only accepted while the program is in REVIEW, οι αξιολογήσεις γίνονται μόνο στο REVIEW.");
        }
        if (screening.getState() != ScreeningState.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    "Only SUBMITTED screenings can be reviewed, αξιολογούνται μόνο προβολές σε κατάσταση SUBMITTED.");
        }
        screening.setReviewScore(dto.getScore());
        screening.setReviewComments(dto.getComments());
        screening.setState(ScreeningState.REVIEWED);
        return toResponseDTO(screening, currentUsername);
    }

    // Παραδοχή: η έγκριση/απόρριψη στο SCHEDULING αποτελεί απόφαση του PROGRAMMER
    // μετά την αξιολόγηση του STAFF, σύμφωνα με τον πίνακα ρόλων. Η κυριολεκτική
    // (και εδώ αντιφατική) διατύπωση "by a SUBMITTER" στη λίστα λειτουργιών δεν
    // ακολουθείται.
    @Transactional
    public ScreeningResponseDTO approve(Long screeningId, ApprovalDTO dto, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        Program program = screening.getProgram();
        requireProgrammer(program, currentUsername);

        if (program.getState() != ProgramState.SCHEDULING) {
            throw new InvalidStateTransitionException(
                    "Approval is only allowed during SCHEDULING, η έγκριση γίνεται μόνο στο SCHEDULING.");
        }
        if (screening.getState() != ScreeningState.REVIEWED) {
            throw new InvalidStateTransitionException(
                    "Only REVIEWED screenings can be approved, εγκρίνονται μόνο προβολές σε κατάσταση REVIEWED.");
        }
        screening.setState(ScreeningState.APPROVED);
        if (dto != null && dto.getConditionalNotes() != null) {
            screening.setReviewComments(screening.getReviewComments() == null
                    ? dto.getConditionalNotes()
                    : screening.getReviewComments() + " | " + dto.getConditionalNotes());
        }
        return toResponseDTO(screening, currentUsername);
    }

    @Transactional
    public ScreeningResponseDTO rejectManual(Long screeningId, RejectionDTO dto, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        Program program = screening.getProgram();
        requireProgrammer(program, currentUsername);

        boolean validInScheduling = program.getState() == ProgramState.SCHEDULING
                && screening.getState() == ScreeningState.REVIEWED;
        boolean validInDecision = program.getState() == ProgramState.DECISION
                && screening.getState() == ScreeningState.APPROVED;

        if (!validInScheduling && !validInDecision) {
            throw new InvalidStateTransitionException(
                    "Rejection is only allowed in SCHEDULING (after review) or DECISION (failed final submission), "
                            + "η απόρριψη επιτρέπεται μόνο σε SCHEDULING ή DECISION.");
        }
        screening.setState(ScreeningState.REJECTED);
        screening.setRejectionReason(dto.getReason());
        return toResponseDTO(screening, currentUsername);
    }

    @Transactional
    public ScreeningResponseDTO finalSubmit(Long screeningId, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        requireSubmitter(screening, currentUsername);
        Program program = screening.getProgram();

        if (program.getState() != ProgramState.FINAL_SUBMISSION) {
            throw new InvalidStateTransitionException(
                    "Final submission is only allowed during FINAL_SUBMISSION, "
                            + "η τελική υποβολή γίνεται μόνο στο FINAL_SUBMISSION.");
        }
        if (screening.getState() != ScreeningState.APPROVED) {
            throw new InvalidStateTransitionException(
                    "Only APPROVED screenings can be finally submitted, τελικά υποβάλλονται μόνο εγκεκριμένες προβολές.");
        }
        screening.setFinallySubmitted(true);
        return toResponseDTO(screening, currentUsername);
    }

    @Transactional
    public ScreeningResponseDTO accept(Long screeningId, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        Program program = screening.getProgram();
        requireProgrammer(program, currentUsername);

        if (program.getState() != ProgramState.DECISION) {
            throw new InvalidStateTransitionException(
                    "Acceptance into the schedule only happens during DECISION, η οριστικοποίηση γίνεται μόνο στο DECISION.");
        }
        if (screening.getState() != ScreeningState.APPROVED || !Boolean.TRUE.equals(screening.getFinallySubmitted())) {
            throw new InvalidStateTransitionException(
                    "Only approved and finally submitted screenings can be scheduled, "
                            + "προγραμματίζονται μόνο εγκεκριμένες και τελικά υποβεβλημένες προβολές.");
        }
        screening.setState(ScreeningState.SCHEDULED);
        return toResponseDTO(screening, currentUsername);
    }

    public List<ScreeningResponseDTO> search(Long programId, String title, String cast, String genre,
                                              LocalDateTime from, LocalDateTime to, boolean timetableView,
                                              String currentUsername) {
        Program program = getProgramOrThrow(programId);

        Specification<Screening> spec = Specification.where(ScreeningSpecifications.inProgram(program))
                .and(ScreeningSpecifications.titleContainsAllWords(title))
                .and(ScreeningSpecifications.castContainsAllWords(cast))
                .and(ScreeningSpecifications.genreContainsAllWords(genre))
                .and(ScreeningSpecifications.startsFrom(from))
                .and(ScreeningSpecifications.startsUntil(to));

        List<Screening> results = screeningRepository.findAll(spec);

        Comparator<Screening> comparator = timetableView
                ? Comparator.comparing(Screening::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing((Screening s) -> s.getFilmGenres() == null ? "" : s.getFilmGenres())
                    .thenComparing(s -> s.getFilmTitle() == null ? "" : s.getFilmTitle());

        return results.stream()
                .filter(s -> canView(s, currentUsername))
                .sorted(comparator)
                .map(s -> toResponseDTO(s, currentUsername))
                .collect(Collectors.toList());
    }

    public ScreeningResponseDTO view(Long screeningId, String currentUsername) {
        Screening screening = getScreeningOrThrow(screeningId);
        if (!canView(screening, currentUsername)) {
            throw new UnauthorizedActionException("Not allowed to view this screening, δεν επιτρέπεται η προβολή.");
        }
        return toResponseDTO(screening, currentUsername);
    }

    private boolean canView(Screening screening, String currentUsername) {
        if (screening.getState() == ScreeningState.SCHEDULED) {
            return true;
        }
        return hasFullAccess(screening, currentUsername);
    }

    private boolean hasFullAccess(Screening screening, String currentUsername) {
        if (currentUsername == null) {
            return false;
        }
        if (screening.getSubmitter().getUsername().equals(currentUsername)) {
            return true;
        }
        if (screening.getHandler() != null && screening.getHandler().getUsername().equals(currentUsername)) {
            return true;
        }
        return userRepository.findByUsername(currentUsername)
                .map(u -> userProgramRoleRepository.existsByUserAndProgramAndRole(
                        u, screening.getProgram(), ProgramRoleType.PROGRAMMER))
                .orElse(false);
    }

    private void requireSubmitter(Screening screening, String currentUsername) {
        if (currentUsername == null || !screening.getSubmitter().getUsername().equals(currentUsername)) {
            throw new UnauthorizedActionException(
                    "Only the SUBMITTER of this screening may perform this action, "
                            + "μόνο ο SUBMITTER αυτής της προβολής επιτρέπεται.");
        }
    }

    private void requireProgrammer(Program program, String currentUsername) {
        User user = requireAuthenticated(currentUsername);
        boolean isProgrammer = userProgramRoleRepository.existsByUserAndProgramAndRole(
                user, program, ProgramRoleType.PROGRAMMER);
        if (!isProgrammer) {
            throw new UnauthorizedActionException(
                    "Only a PROGRAMMER of this program may perform this action, μόνο PROGRAMMER επιτρέπεται.");
        }
    }

    private User requireAuthenticated(String currentUsername) {
        if (currentUsername == null) {
            throw new UnauthorizedActionException("Authentication required, απαιτείται σύνδεση χρήστη.");
        }
        return getUserOrThrow(currentUsername);
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Program getProgramOrThrow(Long programId) {
        return programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found: " + programId));
    }

    private Screening getScreeningOrThrow(Long screeningId) {
        return screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening not found: " + screeningId));
    }

    private ScreeningResponseDTO toResponseDTO(Screening screening, String currentUsername) {
        boolean fullAccess = hasFullAccess(screening, currentUsername);

        return new ScreeningResponseDTO(
                screening.getId(),
                screening.getProgram().getId(),
                screening.getState(),
                screening.getFilmTitle(),
                fullAccess ? screening.getFilmCast() : null,
                screening.getFilmGenres(),
                fullAccess ? screening.getFilmDurationMinutes() : null,
                screening.getAuditoriumName(),
                screening.getStartTime(),
                fullAccess ? screening.getEndTime() : null,
                fullAccess ? screening.getSubmitter().getUsername() : null,
                fullAccess && screening.getHandler() != null ? screening.getHandler().getUsername() : null,
                fullAccess ? screening.getReviewScore() : null,
                fullAccess ? screening.getReviewComments() : null,
                fullAccess ? screening.getRejectionReason() : null
        );
    }
}
