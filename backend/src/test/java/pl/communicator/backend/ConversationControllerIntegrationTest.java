package pl.communicator.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import pl.communicator.backend.model.Conversation;
import pl.communicator.backend.model.ConversationType;
import pl.communicator.backend.model.Role;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.ConversationRepository;
import pl.communicator.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearDatabase() {
        conversationRepository.deleteAll();
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

    private User createUser(String email, String login, String displayName, String password) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setDisplayName(displayName);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        user.setAvatarUrl(null);
        return userRepository.save(user);
    }

    @Test
    void shouldReturnUnauthorizedForCreatePrivateConversationWithoutToken() throws Exception {
        User targetUser = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Map<String, String> request = Map.of(
                "targetUserId", targetUser.getId()
        );

        mockMvc.perform(post("/api/conversations/private")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreatePrivateConversationSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User targetUser = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Map<String, String> request = Map.of(
                "targetUserId", targetUser.getId()
        );

        mockMvc.perform(post("/api/conversations/private")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PRIVATE"))
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andExpect(jsonPath("$.participants[*].login").value(org.hamcrest.Matchers.containsInAnyOrder("janek123", "adam123")));
    }

    @Test
    void shouldReturnExistingConversationInsteadOfCreatingDuplicate() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User targetUser = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Map<String, String> request = Map.of(
                "targetUserId", targetUser.getId()
        );

        String firstResponse = mockMvc.perform(post("/api/conversations/private")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/conversations/private")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstId = objectMapper.readTree(firstResponse).get("id").asText();
        String secondId = objectMapper.readTree(secondResponse).get("id").asText();

        org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId);
        org.junit.jupiter.api.Assertions.assertEquals(1, conversationRepository.count());
    }

    @Test
    void shouldReturnBadRequestWhenCreatingConversationWithYourself() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User currentUser = userRepository.findByLogin("janek123").orElseThrow();

        Map<String, String> request = Map.of(
                "targetUserId", currentUser.getId()
        );

        mockMvc.perform(post("/api/conversations/private")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot create a conversation with yourself"));
    }

    @Test
    void shouldReturnUnauthorizedForMyConversationsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/conversations/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnMyConversations() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User currentUser = userRepository.findByLogin("janek123").orElseThrow();
        User targetUser = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = new Conversation();

        Instant now = Instant.now();

        conversation.setType(ConversationType.PRIVATE);
        conversation.setParticipantIds(List.of(currentUser.getId(), targetUser.getId()));
        conversation.setCreatedBy(currentUser.getId());
        conversation.setCreatedAt(Instant.now());
        conversation.setLastActivityAt(now);
        conversation.setLastMessageId(null);
        conversationRepository.save(conversation);

        mockMvc.perform(get("/api/conversations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("PRIVATE"))
                .andExpect(jsonPath("$[0].participants", hasSize(2)));
    }

    @Test
    void shouldReturnOnlyCurrentUsersConversations() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation myConversation = new Conversation();

        Instant now = Instant.now();

        myConversation.setType(ConversationType.PRIVATE);
        myConversation.setParticipantIds(List.of(janek.getId(), adam.getId()));
        myConversation.setCreatedBy(janek.getId());
        myConversation.setCreatedAt(Instant.now());
        myConversation.setLastActivityAt(now);
        myConversation.setLastMessageId(null);
        conversationRepository.save(myConversation);

        Conversation otherConversation = new Conversation();

        now = Instant.now();

        otherConversation.setType(ConversationType.PRIVATE);
        otherConversation.setParticipantIds(List.of(adam.getId(), kasia.getId()));
        otherConversation.setCreatedBy(adam.getId());
        otherConversation.setCreatedAt(Instant.now());
        otherConversation.setLastActivityAt(now);
        otherConversation.setLastMessageId(null);
        conversationRepository.save(otherConversation);

        mockMvc.perform(get("/api/conversations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].participants[*].login", not(empty())));
    }
}