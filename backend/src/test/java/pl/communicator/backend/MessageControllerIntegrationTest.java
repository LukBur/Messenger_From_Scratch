package pl.communicator.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import pl.communicator.backend.model.Conversation;
import pl.communicator.backend.model.ConversationType;
import pl.communicator.backend.model.Message;
import pl.communicator.backend.model.Role;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.ConversationRepository;
import pl.communicator.backend.repository.MessageRepository;
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
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearDatabase() {
        messageRepository.deleteAll();
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

    private Conversation createPrivateConversation(User firstUser, User secondUser) {
        Conversation conversation = new Conversation();

        Instant now = Instant.now();

        conversation.setType(ConversationType.PRIVATE);
        conversation.setParticipantIds(List.of(firstUser.getId(), secondUser.getId()));
        conversation.setCreatedBy(firstUser.getId());
        conversation.setCreatedAt(Instant.now());
        conversation.setLastActivityAt(now);
        conversation.setLastMessageId(null);
        return conversationRepository.save(conversation);
    }

    private Conversation createGroupConversation(User owner, List<User> participants, String name) {
        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.GROUP);
        conversation.setName(name);
        conversation.setOwnerId(owner.getId());
        conversation.setParticipantIds(participants.stream().map(User::getId).toList());
        conversation.setCreatedBy(owner.getId());
        conversation.setCreatedAt(Instant.now());
        conversation.setLastActivityAt(conversation.getCreatedAt());
        conversation.setLastMessageId(null);

        return conversationRepository.save(conversation);
    }

    @Test
    void shouldReturnUnauthorizedForSendMessageWithoutToken() throws Exception {
        User janek = createUser("janek@test.pl", "janek123", "Janek", "haslo123");
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        Conversation conversation = createPrivateConversation(janek, adam);

        Map<String, String> request = Map.of(
                "conversationId", conversation.getId(),
                "content", "Hello"
        );

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSendMessageSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        Conversation conversation = createPrivateConversation(janek, adam);

        Map<String, String> request = Map.of(
                "conversationId", conversation.getId(),
                "content", "Hello Adam"
        );

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversation.getId()))
                .andExpect(jsonPath("$.content").value("Hello Adam"))
                .andExpect(jsonPath("$.edited").value(false))
                .andExpect(jsonPath("$.sender.login").value("janek123"))
                .andExpect(jsonPath("$.sender.displayName").value("Janek"));
    }

    @Test
    void shouldReturnBadRequestWhenUserIsNotConversationParticipant() throws Exception {
        String token = registerAndLogin("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        User janek = createUser("janek@test.pl", "janek123", "Janek", "haslo123");
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        Conversation conversation = createPrivateConversation(janek, adam);

        Map<String, String> request = Map.of(
                "conversationId", conversation.getId(),
                "content", "I should not send this"
        );

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You are not a participant of this conversation"));
    }

    @Test
    void shouldReturnBadRequestForBlankMessageContent() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        Conversation conversation = createPrivateConversation(janek, adam);

        Map<String, String> request = Map.of(
                "conversationId", conversation.getId(),
                "content", "   "
        );

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Message content is required"));
    }

    @Test
    void shouldReturnUnauthorizedForGetMessagesWithoutToken() throws Exception {
        User janek = createUser("janek@test.pl", "janek123", "Janek", "haslo123");
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        Conversation conversation = createPrivateConversation(janek, adam);

        mockMvc.perform(get("/api/conversations/{conversationId}/messages", conversation.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnConversationMessagesForParticipant() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        Conversation conversation = createPrivateConversation(janek, adam);

        Message firstMessage = new Message();
        firstMessage.setConversationId(conversation.getId());
        firstMessage.setSenderId(janek.getId());
        firstMessage.setContent("First message");
        firstMessage.setCreatedAt(Instant.now().minusSeconds(10));
        firstMessage.setEdited(false);
        firstMessage.setEditedAt(null);
        messageRepository.save(firstMessage);

        Message secondMessage = new Message();
        secondMessage.setConversationId(conversation.getId());
        secondMessage.setSenderId(adam.getId());
        secondMessage.setContent("Second message");
        secondMessage.setCreatedAt(Instant.now());
        secondMessage.setEdited(false);
        secondMessage.setEditedAt(null);
        messageRepository.save(secondMessage);

        mockMvc.perform(get("/api/conversations/{conversationId}/messages", conversation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].content", contains("First message", "Second message")))
                .andExpect(jsonPath("$[0].sender.login").value("janek123"))
                .andExpect(jsonPath("$[1].sender.login").value("adam123"));
    }

    @Test
    void shouldReturnBadRequestWhenNonParticipantGetsMessages() throws Exception {
        String token = registerAndLogin("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        User janek = createUser("janek@test.pl", "janek123", "Janek", "haslo123");
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        Conversation conversation = createPrivateConversation(janek, adam);

        mockMvc.perform(get("/api/conversations/{conversationId}/messages", conversation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You are not a participant of this conversation"));
    }

    @Test
    void shouldEditMessageSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = createPrivateConversation(janek, adam);

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(janek.getId());
        message.setContent("Old content");
        message.setCreatedAt(Instant.now());
        message.setEdited(false);
        message.setEditedAt(null);
        message = messageRepository.save(message);

        Map<String, String> request = Map.of(
                "content", "Updated content"
        );

        mockMvc.perform(put("/api/messages/" + message.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(message.getId()))
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.edited").value(true))
                .andExpect(jsonPath("$.editedAt").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenEditingOtherUsersMessage() throws Exception {
        String janekToken = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        String adamToken = registerAndLogin("adam@test.pl", "adam123", "Adam", "haslo456");

        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = userRepository.findByLogin("adam123").orElseThrow();

        Conversation conversation = createPrivateConversation(janek, adam);

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(adam.getId());
        message.setContent("Adam message");
        message.setCreatedAt(Instant.now());
        message.setEdited(false);
        message.setEditedAt(null);
        messageRepository.save(message);

        Map<String, String> request = Map.of(
                "content", "Illegal edit"
        );

        mockMvc.perform(put("/api/messages/" + message.getId())
                        .header("Authorization", "Bearer " + janekToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSendDisappearingMessageSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = createPrivateConversation(janek, adam);

        Map<String, Object> request = Map.of(
                "conversationId", conversation.getId(),
                "content", "This will disappear",
                "disappearAfterSeconds", 30
        );

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("This will disappear"))
                .andExpect(jsonPath("$.disappearing").value(true))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void shouldReturnMessageWithExpiresAtWhenDisappearingMessageSent() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = createPrivateConversation(janek, adam);

        Map<String, Object> request = Map.of(
                "conversationId", conversation.getId(),
                "content", "Short lived",
                "disappearAfterSeconds", 10
        );

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disappearing").value(true))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenEditingExpiredMessage() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = createPrivateConversation(janek, adam);

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(janek.getId());
        message.setContent("Expired");
        message.setCreatedAt(Instant.now().minusSeconds(20));
        message.setEdited(false);
        message.setEditedAt(null);
        message.setDisappearing(true);
        message.setExpiresAt(Instant.now().minusSeconds(5));
        message = messageRepository.save(message);

        Map<String, String> request = Map.of(
                "content", "Updated expired"
        );

        mockMvc.perform(put("/api/messages/" + message.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteOwnMessageSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = createPrivateConversation(janek, adam);

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(janek.getId());
        message.setContent("Delete me");
        message.setCreatedAt(Instant.now());
        message.setEdited(false);
        message.setEditedAt(null);
        message = messageRepository.save(message);

        mockMvc.perform(delete("/api/messages/" + message.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertFalse(
                messageRepository.findById(message.getId()).isPresent()
        );
    }

    @Test
    void shouldReturnBadRequestWhenUserDeletesOtherUsersPrivateMessage() throws Exception {
        String janekToken = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        String adamToken = registerAndLogin("adam@test.pl", "adam123", "Adam", "haslo456");

        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = userRepository.findByLogin("adam123").orElseThrow();

        Conversation conversation = createPrivateConversation(janek, adam);

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(adam.getId());
        message.setContent("Adam message");
        message.setCreatedAt(Instant.now());
        message.setEdited(false);
        message.setEditedAt(null);
        message = messageRepository.save(message);

        mockMvc.perform(delete("/api/messages/" + message.getId())
                        .header("Authorization", "Bearer " + janekToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowGroupOwnerToDeleteOtherUsersMessageInGroup() throws Exception {
        String ownerToken = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        String adamToken = registerAndLogin("adam@test.pl", "adam123", "Adam", "haslo456");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = userRepository.findByLogin("adam123").orElseThrow();
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Projekt ATI"
        );

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(adam.getId());
        message.setContent("Owner can delete this");
        message.setCreatedAt(Instant.now());
        message.setEdited(false);
        message.setEditedAt(null);
        message = messageRepository.save(message);

        mockMvc.perform(delete("/api/messages/" + message.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertFalse(
                messageRepository.findById(message.getId()).isPresent()
        );
    }

    @Test
    void shouldNotReturnExpiredMessagesInConversationHistory() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = createPrivateConversation(janek, adam);

        Message expiredMessage = new Message();
        expiredMessage.setConversationId(conversation.getId());
        expiredMessage.setSenderId(janek.getId());
        expiredMessage.setContent("Expired message");
        expiredMessage.setCreatedAt(Instant.now().minusSeconds(30));
        expiredMessage.setEdited(false);
        expiredMessage.setEditedAt(null);
        expiredMessage.setDisappearing(true);
        expiredMessage.setExpiresAt(Instant.now().minusSeconds(5));
        messageRepository.save(expiredMessage);

        Message activeMessage = new Message();
        activeMessage.setConversationId(conversation.getId());
        activeMessage.setSenderId(adam.getId());
        activeMessage.setContent("Active message");
        activeMessage.setCreatedAt(Instant.now());
        activeMessage.setEdited(false);
        activeMessage.setEditedAt(null);
        activeMessage.setDisappearing(false);
        activeMessage.setExpiresAt(null);
        messageRepository.save(activeMessage);

        mockMvc.perform(get("/api/conversations/{conversationId}/messages", conversation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Active message"));
    }
}