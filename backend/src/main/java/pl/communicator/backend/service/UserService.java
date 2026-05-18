package pl.communicator.backend.service;

import org.springframework.stereotype.Service;
import pl.communicator.backend.dto.UpdateProfileRequest;
import pl.communicator.backend.dto.UserResponse;
import pl.communicator.backend.exception.ResourceNotFoundException;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getCurrentUser(String currentLogin) {
        User user = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        return mapToResponse(user);
    }

    public UserResponse updateProfile(String currentLogin, UpdateProfileRequest request) {
        User user = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        String trimmedDisplayName = request.getDisplayName().trim();
        if (trimmedDisplayName.isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }

        String avatarUrl = request.getAvatarUrl();
        if (avatarUrl != null) {
            avatarUrl = avatarUrl.trim();
            if (avatarUrl.isEmpty()) {
                avatarUrl = null;
            }
        }

        user.setDisplayName(trimmedDisplayName);
        user.setAvatarUrl(avatarUrl);

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getLogin(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getAvatarUrl()
        );
    }
}