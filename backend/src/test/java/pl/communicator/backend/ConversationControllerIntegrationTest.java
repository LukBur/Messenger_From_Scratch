package pl.communicator.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import pl.communicator.backend.model.*;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    void shouldReturnConversationWithNullLastMessageWhenNoMessagesExist() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User currentUser = userRepository.findByLogin("janek123").orElseThrow();
        User targetUser = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.PRIVATE);
        conversation.setParticipantIds(List.of(currentUser.getId(), targetUser.getId()));
        conversation.setCreatedBy(currentUser.getId());
        conversation.setCreatedAt(Instant.now());
        conversation.setLastActivityAt(conversation.getCreatedAt());
        conversation.setLastMessageId(null);
        conversationRepository.save(conversation);

        mockMvc.perform(get("/api/conversations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lastMessage").doesNotExist())
                .andExpect(jsonPath("$[0].lastActivityAt").isNotEmpty());
    }

    @Test
    void shouldReturnLastMessageAndLastActivityAtForConversation() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User currentUser = userRepository.findByLogin("janek123").orElseThrow();
        User targetUser = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.PRIVATE);
        conversation.setParticipantIds(List.of(currentUser.getId(), targetUser.getId()));
        conversation.setCreatedBy(currentUser.getId());
        conversation.setCreatedAt(Instant.now().minusSeconds(60));
        conversation.setLastActivityAt(conversation.getCreatedAt());
        conversation.setLastMessageId(null);
        conversation = conversationRepository.save(conversation);

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(currentUser.getId());
        message.setContent("Hello Adam");
        message.setCreatedAt(Instant.now());
        message.setEdited(false);
        message.setEditedAt(null);
        message = messageRepository.save(message);

        conversation.setLastActivityAt(message.getCreatedAt());
        conversation.setLastMessageId(message.getId());
        conversationRepository.save(conversation);

        mockMvc.perform(get("/api/conversations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lastActivityAt").isNotEmpty())
                .andExpect(jsonPath("$[0].lastMessage.content").value("Hello Adam"))
                .andExpect(jsonPath("$[0].lastMessage.senderLogin").value("janek123"))
                .andExpect(jsonPath("$[0].lastMessage.senderDisplayName").value("Janek"))
                .andExpect(jsonPath("$[0].lastMessage.edited").value(false));
    }

    @Test
    void shouldReturnConversationsSortedByLastActivityAtDescending() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User janek = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation olderConversation = new Conversation();
        olderConversation.setType(ConversationType.PRIVATE);
        olderConversation.setParticipantIds(List.of(janek.getId(), adam.getId()));
        olderConversation.setCreatedBy(janek.getId());
        olderConversation.setCreatedAt(Instant.now().minusSeconds(120));
        olderConversation.setLastActivityAt(Instant.now().minusSeconds(60));
        olderConversation.setLastMessageId(null);
        olderConversation = conversationRepository.save(olderConversation);

        Conversation newerConversation = new Conversation();
        newerConversation.setType(ConversationType.PRIVATE);
        newerConversation.setParticipantIds(List.of(janek.getId(), kasia.getId()));
        newerConversation.setCreatedBy(janek.getId());
        newerConversation.setCreatedAt(Instant.now().minusSeconds(100));
        newerConversation.setLastActivityAt(Instant.now().minusSeconds(10));
        newerConversation.setLastMessageId(null);
        newerConversation = conversationRepository.save(newerConversation);

        mockMvc.perform(get("/api/conversations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(newerConversation.getId()))
                .andExpect(jsonPath("$[1].id").value(olderConversation.getId()));
    }

    @Test
    void shouldCreateGroupConversationSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Map<String, Object> request = Map.of(
                "name", "Projekt ATI",
                "participantIds", List.of(adam.getId(), kasia.getId())
        );

        mockMvc.perform(post("/api/conversations/group")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.name").value("Projekt ATI"))
                .andExpect(jsonPath("$.participants", hasSize(3)))
                .andExpect(jsonPath("$.ownerId").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenGroupHasLessThanThreeParticipants() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");

        Map<String, Object> request = Map.of(
                "name", "Za mała grupa",
                "participantIds", List.of(adam.getId())
        );

        mockMvc.perform(post("/api/conversations/group")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateGroupNameSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Stara nazwa"
        );

        Map<String, String> request = Map.of(
                "name", "Nowa nazwa grupy"
        );

        mockMvc.perform(put("/api/conversations/" + conversation.getId() + "/group/name")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nowa nazwa grupy"))
                .andExpect(jsonPath("$.type").value("GROUP"));
    }

    @Test
    void shouldReturnBadRequestWhenNonOwnerUpdatesGroupName() throws Exception {
        String ownerToken = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        String memberToken = registerAndLogin("adam@test.pl", "adam123", "Adam", "haslo456");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User member = userRepository.findByLogin("adam123").orElseThrow();
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, member, kasia),
                "Stara nazwa"
        );

        Map<String, String> request = Map.of(
                "name", "Nielegalna zmiana"
        );

        mockMvc.perform(put("/api/conversations/" + conversation.getId() + "/group/name")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAddParticipantToGroupSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");
        User ola = createUser("ola@test.pl", "ola123", "Ola", "haslo999");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Projekt ATI"
        );

        Map<String, String> request = Map.of(
                "userId", ola.getId()
        );

        mockMvc.perform(post("/api/conversations/" + conversation.getId() + "/group/participants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants", hasSize(4)))
                .andExpect(jsonPath("$.participants[*].login")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("janek123", "adam123", "kasia123", "ola123")));
    }

    @Test
    void shouldReturnBadRequestWhenAddingExistingParticipantToGroup() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Projekt ATI"
        );

        Map<String, String> request = Map.of(
                "userId", adam.getId()
        );

        mockMvc.perform(post("/api/conversations/" + conversation.getId() + "/group/participants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRemoveParticipantFromGroupSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");
        User ola = createUser("ola@test.pl", "ola123", "Ola", "haslo999");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia, ola),
                "Projekt ATI"
        );

        Map<String, String> request = Map.of(
                "userId", ola.getId()
        );

        mockMvc.perform(delete("/api/conversations/" + conversation.getId() + "/group/participants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants", hasSize(3)))
                .andExpect(jsonPath("$.participants[*].login")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("janek123", "adam123", "kasia123")));
    }

    @Test
    void shouldReturnBadRequestWhenOwnerTriesToRemoveSelf() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");
        User ola = createUser("ola@test.pl", "ola123", "Ola", "haslo999");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia, ola),
                "Projekt ATI"
        );

        Map<String, String> request = Map.of(
                "userId", owner.getId()
        );

        mockMvc.perform(delete("/api/conversations/" + conversation.getId() + "/group/participants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRemovingParticipantWouldBreakMinimumGroupSize() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Projekt ATI"
        );

        Map<String, String> request = Map.of(
                "userId", adam.getId()
        );

        mockMvc.perform(delete("/api/conversations/" + conversation.getId() + "/group/participants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLeaveGroupSuccessfullyForNonOwner() throws Exception {
        String ownerToken = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        String memberToken = registerAndLogin("adam@test.pl", "adam123", "Adam", "haslo456");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User member = userRepository.findByLogin("adam123").orElseThrow();
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, member, kasia),
                "Projekt ATI"
        );

        mockMvc.perform(post("/api/conversations/" + conversation.getId() + "/group/leave")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        Conversation updatedConversation = conversationRepository.findById(conversation.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(2, updatedConversation.getParticipantIds().size());
        org.junit.jupiter.api.Assertions.assertFalse(updatedConversation.getParticipantIds().contains(member.getId()));
    }

    @Test
    void shouldReturnBadRequestWhenOwnerTriesToLeaveGroupWithoutTransfer() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Projekt ATI"
        );

        mockMvc.perform(post("/api/conversations/" + conversation.getId() + "/group/leave")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldTransferGroupOwnershipSuccessfully() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Projekt ATI"
        );

        Map<String, String> request = Map.of(
                "newOwnerId", adam.getId()
        );

        mockMvc.perform(put("/api/conversations/" + conversation.getId() + "/group/owner")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(adam.getId()));
    }

    @Test
    void shouldReturnBadRequestWhenNewOwnerIsNotParticipant() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");
        User outsider = createUser("ola@test.pl", "ola123", "Ola", "haslo999");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Projekt ATI"
        );

        Map<String, String> request = Map.of(
                "newOwnerId", outsider.getId()
        );

        mockMvc.perform(put("/api/conversations/" + conversation.getId() + "/group/owner")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteGroupSuccessfullyForOwner() throws Exception {
        String token = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User adam = createUser("adam@test.pl", "adam123", "Adam", "haslo456");
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, adam, kasia),
                "Projekt ATI"
        );

        mockMvc.perform(delete("/api/conversations/" + conversation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertFalse(
                conversationRepository.findById(conversation.getId()).isPresent()
        );
    }

    @Test
    void shouldReturnBadRequestWhenNonOwnerDeletesGroup() throws Exception {
        String ownerToken = registerAndLogin("janek@test.pl", "janek123", "Janek", "haslo123");
        String memberToken = registerAndLogin("adam@test.pl", "adam123", "Adam", "haslo456");

        User owner = userRepository.findByLogin("janek123").orElseThrow();
        User member = userRepository.findByLogin("adam123").orElseThrow();
        User kasia = createUser("kasia@test.pl", "kasia123", "Kasia", "haslo789");

        Conversation conversation = createGroupConversation(
                owner,
                List.of(owner, member, kasia),
                "Projekt ATI"
        );

        mockMvc.perform(delete("/api/conversations/" + conversation.getId())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isBadRequest());
    }
}