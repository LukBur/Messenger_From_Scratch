package pl.communicator.backend.service;

import pl.communicator.backend.dto.LoginRequest;
import pl.communicator.backend.dto.RegisterRequest;
import pl.communicator.backend.exception.InvalidCredentialsException;
import pl.communicator.backend.exception.UserAlreadyExistsException;
import pl.communicator.backend.model.Role;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.UserRepository;
import pl.communicator.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // Registers a new user after checking that email and login are unique.
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already taken");
        }

        if (userRepository.existsByLogin(request.getLogin())) {
            throw new UserAlreadyExistsException("Login is already taken");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setLogin(request.getLogin());
        user.setDisplayName(request.getDisplayName());

        // The password is encoded before saving, so the raw password is never stored.
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(Role.USER);
        user.setAvatarUrl(null);

        userRepository.save(user);

        return "Registration successful";
    }

    // Authenticates the user and returns a JWT token if the credentials are valid.
    public String login(LoginRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid login or password"));

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatches) {
            throw new InvalidCredentialsException("Invalid login or password");
        }

        return jwtService.generateToken(user.getLogin());
    }
}