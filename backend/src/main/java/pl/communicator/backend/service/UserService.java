package pl.communicator.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.communicator.backend.dto.ChangePasswordRequest;
import pl.communicator.backend.dto.UpdateProfileRequest;
import pl.communicator.backend.dto.UserResponse;
import pl.communicator.backend.exception.ResourceNotFoundException;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Returns profile data for the currently authenticated user.
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

        // Blank avatar URLs are stored as null to avoid keeping meaningless values.
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

    public void changePassword(String currentLogin, ChangePasswordRequest request) {
        User user = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        // The current password must be verified before allowing the user to change it.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        String trimmedNewPassword = request.getNewPassword().trim();

        if (trimmedNewPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long");
        }

        if (passwordEncoder.matches(trimmedNewPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        // The new password is encoded before saving, so the raw password is never stored.
        user.setPassword(passwordEncoder.encode(trimmedNewPassword));
        userRepository.save(user);
    }

    // Converts the user entity into a response object without exposing the password hash.
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