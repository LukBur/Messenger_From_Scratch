package pl.communicator.backend.dto;

import lombok.Getter;

@Getter
public class UserResponse {

    private String id;
    private String email;
    private String login;
    private String displayName;
    private String role;
    private String avatarUrl;

    public UserResponse() {
    }

    public UserResponse(String id, String email, String login, String displayName, String role, String avatarUrl) {
        this.id = id;
        this.email = email;
        this.login = login;
        this.displayName = displayName;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }

}