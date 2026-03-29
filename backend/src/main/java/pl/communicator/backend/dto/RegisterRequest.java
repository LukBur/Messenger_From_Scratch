package pl.communicator.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {

    @Email(message = "Niepoprawny email")
    @NotBlank(message = "Email jest wymagany")
    private String email;

    @NotBlank(message = "Login jest wymagany")
    private String login;

    @NotBlank(message = "Display name jest wymagany")
    private String displayName;

    @NotBlank(message = "Haslo jest wymagane")
    private String password;

    public RegisterRequest() {
    }

}