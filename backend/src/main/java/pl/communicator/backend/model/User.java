package pl.communicator.backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Setter
    private String email;

    // Unique username used for login and identifying users in the application.
    @Setter
    private String login;

    // Public name displayed in conversations and search results.
    @Setter
    private String displayName;

    // Stores the hashed password, not the plain text value.
    @Setter
    private String password;

    // Defines the user's permissions in the application.
    @Setter
    private Role role;

    // Optional URL to the user's profile image.
    @Setter
    private String avatarUrl;

    public User() {
    }

    public User(String email, String login, String displayName, String password, Role role, String avatarUrl) {
        this.email = email;
        this.login = login;
        this.displayName = displayName;
        this.password = password;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }

}