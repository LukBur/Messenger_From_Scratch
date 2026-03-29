package pl.communicator.backend.controller;

import pl.communicator.backend.dto.AuthResponse;
import pl.communicator.backend.dto.RegisterRequest;
import pl.communicator.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return new AuthResponse(message);
    }
}