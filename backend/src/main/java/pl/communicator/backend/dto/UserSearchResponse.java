package pl.communicator.backend.dto;

public class UserSearchResponse {

    private String id;
    private String login;
    private String displayName;
    private String avatarUrl;
    private String role;

    public UserSearchResponse() {
    }

    public UserSearchResponse(String id, String login, String displayName, String avatarUrl, String role) {
        this.id = id;
        this.login = login;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getRole() {
        return role;
    }
}