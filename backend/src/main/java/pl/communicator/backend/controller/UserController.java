package pl.communicator.backend.controller;

import pl.communicator.backend.dto.UserResponse;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
}