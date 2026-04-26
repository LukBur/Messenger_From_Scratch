package pl.communicator.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Login is required")
    private String login;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Password is required")
    private String password;

    public RegisterRequest() {
    }

}