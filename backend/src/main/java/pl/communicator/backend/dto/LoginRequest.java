package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {

    @NotBlank(message = "Login jest wymagany")
    private String login;

    @NotBlank(message = "Haslo jest wymagane")
    private String password;

    public LoginRequest() {
    }

}