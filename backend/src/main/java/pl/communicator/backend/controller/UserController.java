package pl.communicator.backend.controller;

import pl.communicator.backend.dto.UserResponse;
import pl.communicator.backend.dto.UserSearchResponse;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Returns profile data of the currently authenticated user.
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        String login = authentication.getName();

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getLogin(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getAvatarUrl()
        );
    }

    // Searches users by login or display name.
    @GetMapping("/search")
    public List<UserSearchResponse> searchUsers(
            @RequestParam String query,
            Authentication authentication
    ) {
        // Empty queries are ignored to avoid returning unnecessary results.
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String currentLogin = authentication.getName();

        List<User> users = userRepository
                .findByLoginContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query);

        return users.stream()
                // The current user is removed from search results.
                .filter(user -> !user.getLogin().equals(currentLogin))
                .map(user -> new UserSearchResponse(
                        user.getId(),
                        user.getLogin(),
                        user.getDisplayName(),
                        user.getAvatarUrl(),
                        user.getRole().name()
                ))
                .toList();
    }
}