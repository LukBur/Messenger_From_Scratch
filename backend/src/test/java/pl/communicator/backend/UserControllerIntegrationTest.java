package pl.communicator.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import pl.communicator.backend.model.Role;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearDatabase() {
        userRepository.deleteAll();
    }

    private String registerAndLogin(String email, String login, String displayName, String password) throws Exception {
        Map<String, String> registerRequest = Map.of(
                "email", email,
                "login", login,
                "displayName", displayName,
                "password", password
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        Map<String, String> loginRequest = Map.of(
                "login", login,
                "password", password
        );

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("token").asText();
    }

    @Test
    void shouldReturnUnauthorizedForMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUserForMeWithToken() throws Exception {
        String token = registerAndLogin(
                "janek@test.pl",
                "janek123",
                "Janek",
                "haslo123"
        );

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("janek123"))
                .andExpect(jsonPath("$.displayName").value("Janek"))
                .andExpect(jsonPath("$.email").value("janek@test.pl"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldReturnUnauthorizedForSearchWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("query", "jan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSearchUsersByLoginOrDisplayName() throws Exception {
        String token = registerAndLogin(
                "janek@test.pl",
                "janek123",
                "Janek",
                "haslo123"
        );

        User otherUser = new User();
        otherUser.setEmail("adam@test.pl");
        otherUser.setLogin("adam123");
        otherUser.setDisplayName("Adam Nowak");
        otherUser.setPassword(passwordEncoder.encode("haslo456"));
        otherUser.setRole(Role.USER);
        otherUser.setAvatarUrl(null);
        userRepository.save(otherUser);

        mockMvc.perform(get("/api/users/search")
                        .param("query", "adam")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].login").value("adam123"))
                .andExpect(jsonPath("$[0].displayName").value("Adam Nowak"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    void shouldNotReturnCurrentUserInSearchResults() throws Exception {
        String token = registerAndLogin(
                "janek@test.pl",
                "janek123",
                "Janek",
                "haslo123"
        );

        mockMvc.perform(get("/api/users/search")
                        .param("query", "jan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].login", not(hasItem("janek123"))));
    }

    @Test
    void shouldReturnEmptyListForEmptyQuery() throws Exception {
        String token = registerAndLogin(
                "janek@test.pl",
                "janek123",
                "Janek",
                "haslo123"
        );

        mockMvc.perform(get("/api/users/search")
                        .param("query", "")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldUpdateProfileSuccessfully() throws Exception {
        String token = registerAndLogin(
                "dominik@example.com",
                "dominik123",
                "Dominik",
                "password123"
        );

        String requestBody = """
                {
                  "displayName": "Dominik Updated",
                  "avatarUrl": "https://i.pravatar.cc/150?img=12"
                }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Dominik Updated"))
                .andExpect(jsonPath("$.avatarUrl").value("https://i.pravatar.cc/150?img=12"))
                .andExpect(jsonPath("$.login").value("dominik123"))
                .andExpect(jsonPath("$.email").value("dominik@example.com"));
    }

    @Test
    void shouldSetAvatarUrlToNullWhenEmptyStringProvided() throws Exception {
        String token = registerAndLogin(
                "dominik@example.com",
                "dominik123",
                "Dominik",
                "password123"
        );

        String requestBody = """
                {
                  "displayName": "Dominik Updated",
                  "avatarUrl": ""
                }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Dominik Updated"))
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }

    @Test
    void shouldReturnUnauthorizedForUpdateProfileWithoutToken() throws Exception {
        String requestBody = """
                {
                  "displayName": "Dominik Updated",
                  "avatarUrl": "https://i.pravatar.cc/150?img=12"
                }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldChangePasswordSuccessfully() throws Exception {
        String token = registerAndLogin(
                "dominik@example.com",
                "dominik123",
                "Dominik",
                "password123"
        );

        String changePasswordBody = """
                {
                  "currentPassword": "password123",
                  "newPassword": "newPassword123"
                }
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "dominik123",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "dominik123",
                                  "password": "newPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenCurrentPasswordIsIncorrect() throws Exception {
        String token = registerAndLogin(
                "dominik@example.com",
                "dominik123",
                "Dominik",
                "password123"
        );

        String requestBody = """
                {
                  "currentPassword": "wrongPassword",
                  "newPassword": "newPassword123"
                }
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenNewPasswordIsTooShort() throws Exception {
        String token = registerAndLogin(
                "dominik@example.com",
                "dominik123",
                "Dominik",
                "password123"
        );

        String requestBody = """
                {
                  "currentPassword": "password123",
                  "newPassword": "123"
                }
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenNewPasswordIsSameAsCurrentPassword() throws Exception {
        String token = registerAndLogin(
                "dominik@example.com",
                "dominik123",
                "Dominik",
                "password123"
        );

        String requestBody = """
                {
                  "currentPassword": "password123",
                  "newPassword": "password123"
                }
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedForChangePasswordWithoutToken() throws Exception {
        String requestBody = """
                {
                  "currentPassword": "password123",
                  "newPassword": "newPassword123"
                }
                """;

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}