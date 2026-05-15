package pl.communicator.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.communicator.backend.dto.ConversationCreatedEvent;
import pl.communicator.backend.dto.ConversationLastMessageResponse;
import pl.communicator.backend.dto.ConversationParticipantResponse;
import pl.communicator.backend.dto.ConversationResponse;
import pl.communicator.backend.dto.CreateGroupConversationRequest;
import pl.communicator.backend.dto.CreatePrivateConversationRequest;
import pl.communicator.backend.exception.ResourceNotFoundException;
import pl.communicator.backend.model.Conversation;
import pl.communicator.backend.model.ConversationType;
import pl.communicator.backend.model.Message;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.ConversationRepository;
import pl.communicator.backend.repository.MessageRepository;
import pl.communicator.backend.repository.UserRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ConversationService(ConversationRepository conversationRepository,
                               UserRepository userRepository,
                               MessageRepository messageRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Creates a private conversation or returns an existing one for the same two users.
    public ConversationResponse createOrGetPrivateConversation(
            String currentLogin,
            CreatePrivateConversationRequest request
    ) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("You cannot create a conversation with yourself");
        }

        List<Conversation> privateConversations = conversationRepository.findByType(ConversationType.PRIVATE);

        // Prevents creating duplicate private conversations between the same users.
        for (Conversation conversation : privateConversations) {
            List<String> participantIds = conversation.getParticipantIds();

            if (participantIds.size() == 2
                    && participantIds.contains(currentUser.getId())
                    && participantIds.contains(targetUser.getId())) {
                return mapToResponse(conversation);
            }
        }

        Instant now = Instant.now();

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.PRIVATE);
        conversation.setParticipantIds(List.of(currentUser.getId(), targetUser.getId()));
        conversation.setName(null);
        conversation.setOwnerId(null);
        conversation.setCreatedBy(currentUser.getId());
        conversation.setCreatedAt(now);
        conversation.setLastActivityAt(now);
        conversation.setLastMessageId(null);

        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationCreatedEvent event = new ConversationCreatedEvent(
                savedConversation.getId(),
                savedConversation.getType().name(),
                savedConversation.getName()
        );

        messagingTemplate.convertAndSend(
                "/topic/users/" + currentUser.getId() + "/conversations",
                event
        );

        messagingTemplate.convertAndSend(
                "/topic/users/" + targetUser.getId() + "/conversations",
                event
        );

        return mapToResponse(savedConversation);
    }

    public ConversationResponse createGroupConversation(
            String currentLogin,
            CreateGroupConversationRequest request
    ) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        String trimmedName = request.getName().trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }

        Set<String> uniqueParticipantIds = new LinkedHashSet<>(request.getParticipantIds());
        uniqueParticipantIds.add(currentUser.getId());

        if (uniqueParticipantIds.size() < 3) {
            throw new IllegalArgumentException("Group conversation must have at least 3 participants");
        }

        List<User> participants = uniqueParticipantIds.stream()
                .map(userId -> userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + userId)))
                .toList();

        Instant now = Instant.now();

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.GROUP);
        conversation.setParticipantIds(participants.stream().map(User::getId).toList());
        conversation.setName(trimmedName);
        conversation.setOwnerId(currentUser.getId());
        conversation.setCreatedBy(currentUser.getId());
        conversation.setCreatedAt(now);
        conversation.setLastActivityAt(now);
        conversation.setLastMessageId(null);

        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationCreatedEvent event = new ConversationCreatedEvent(
                savedConversation.getId(),
                savedConversation.getType().name(),
                savedConversation.getName()
        );

        for (User participant : participants) {
            messagingTemplate.convertAndSend(
                    "/topic/users/" + participant.getId() + "/conversations",
                    event
            );
        }

        return mapToResponse(savedConversation);
    }

    public List<ConversationResponse> getMyConversations(String currentLogin) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        List<Conversation> conversations =
                conversationRepository.findByParticipantIdsContaining(currentUser.getId());

        return conversations.stream()
                .sorted((a, b) -> {
                    // If there is no last activity date, the creation date is used as a fallback.
                    Instant aTime = a.getLastActivityAt() != null ? a.getLastActivityAt() : a.getCreatedAt();
                    Instant bTime = b.getLastActivityAt() != null ? b.getLastActivityAt() : b.getCreatedAt();
                    return bTime.compareTo(aTime);
                })
                .map(this::mapToResponse)
                .toList();
    }

    // Converts the conversation entity into a response object used by the API.
    private ConversationResponse mapToResponse(Conversation conversation) {
        List<ConversationParticipantResponse> participants = conversation.getParticipantIds().stream()
                .map(userId -> userRepository.findById(userId)
                        .map(user -> new ConversationParticipantResponse(
                                user.getId(),
                                user.getLogin(),
                                user.getDisplayName(),
                                user.getAvatarUrl()
                        ))
                        .orElseThrow(() -> new ResourceNotFoundException("Participant not found")))
                .toList();

        ConversationLastMessageResponse lastMessageResponse = null;

        // Adds last message data only when the conversation already has at least one message.
        if (conversation.getLastMessageId() != null) {
            Message lastMessage = messageRepository.findById(conversation.getLastMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Last message not found"));

            User sender = userRepository.findById(lastMessage.getSenderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

            lastMessageResponse = new ConversationLastMessageResponse(
                    lastMessage.getId(),
                    lastMessage.getContent(),
                    lastMessage.getCreatedAt(),
                    lastMessage.isEdited(),
                    sender.getLogin(),
                    sender.getDisplayName()
            );
        }

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType().name(),
                conversation.getName(),
                conversation.getOwnerId(),
                participants,
                conversation.getCreatedBy(),
                conversation.getCreatedAt(),
                conversation.getLastActivityAt(),
                lastMessageResponse
        );
    }
}