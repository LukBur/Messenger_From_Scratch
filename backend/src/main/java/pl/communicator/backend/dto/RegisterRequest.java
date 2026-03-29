package pl.communicator.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}