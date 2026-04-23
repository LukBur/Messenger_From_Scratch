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
}