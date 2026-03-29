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
    @Setter
    private String login;
    @Setter
    private String displayName;
    @Setter
    private String password;
    @Setter
    private Role role;
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