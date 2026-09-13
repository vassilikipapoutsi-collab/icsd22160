package com.cinema.management.service;

import com.cinema.management.dto.UserRegisterDTO;
import com.cinema.management.dto.UserResponseDTO;
import com.cinema.management.exception.ConflictException;
import com.cinema.management.exception.ResourceNotFoundException;
import com.cinema.management.model.User;
import com.cinema.management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponseDTO register(UserRegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ConflictException("Username already exists, ο χρήστης υπάρχει ήδη.");
        }
        User user = new User(dto.getUsername(), dto.getPassword(), dto.getFullName());
        User saved = userRepository.save(user);
        return toResponseDTO(saved);
    }

    public UserResponseDTO getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toResponseDTO(user);
    }

    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(user.getId(), user.getUsername(), user.getFullName());
    }
}
