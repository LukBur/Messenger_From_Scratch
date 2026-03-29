package pl.communicator.backend.service;

import pl.communicator.backend.dto.LoginRequest;
import pl.communicator.backend.dto.RegisterRequest;
import pl.communicator.backend.exception.InvalidCredentialsException;
import pl.communicator.backend.exception.UserAlreadyExistsException;
import pl.communicator.backend.model.Role;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email jest juz zajety");
        }

        if (userRepository.existsByLogin(request.getLogin())) {
            throw new UserAlreadyExistsException("Login jest juz zajety");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setLogin(request.getLogin());
        user.setDisplayName(request.getDisplayName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setAvatarUrl(null);

        userRepository.save(user);

        return "Rejestracja zakonczona sukcesem";
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new InvalidCredentialsException("Nieprawidlowy login lub haslo"));

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatches) {
            throw new InvalidCredentialsException("Nieprawidlowy login lub haslo");
        }

        return "Logowanie zakonczone sukcesem";
    }
}