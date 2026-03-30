package pl.communicator.backend.controller;

import pl.communicator.backend.dto.AuthResponse;
import pl.communicator.backend.dto.LoginRequest;
import pl.communicator.backend.dto.LoginResponse;
import pl.communicator.backend.dto.RegisterRequest;
import pl.communicator.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return new AuthResponse(message);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return new LoginResponse(token);
    }
}