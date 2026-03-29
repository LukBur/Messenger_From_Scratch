package pl.communicator.backend.service;

import pl.communicator.backend.dto.RegisterRequest;
import pl.communicator.backend.model.Role;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return "Email jest juz zajety";
        }

        if (userRepository.existsByLogin(request.getLogin())) {
            return "Login jest juz zajety";
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setLogin(request.getLogin());
        user.setDisplayName(request.getDisplayName());
        user.setPassword(request.getPassword());
        user.setRole(Role.USER);
        user.setAvatarUrl(null);

        userRepository.save(user);

        return "Rejestracja zakonczona sukcesem";
    }
}