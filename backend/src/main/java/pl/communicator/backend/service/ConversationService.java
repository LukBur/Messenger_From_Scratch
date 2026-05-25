package pl.communicator.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.communicator.backend.dto.*;
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

        // The leaving user gets a separate event so the frontend can remove the conversation locally.
        messagingTemplate.convertAndSend(
                "/topic/users/" + currentUser.getId() + "/conversations",
                event
        );

        // The leaving user gets a separate event so the frontend can remove the conversation locally.
        messagingTemplate.convertAndSend(
                "/topic/users/" + targetUser.getId() + "/conversations",
                event
        );

        sendConversationUpdatedEvent(savedConversation);
        return mapToResponse(savedConversation);
    }

    // Creates a group conversation, removes duplicate participant ids, and makes the creator the owner.
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

        // LinkedHashSet removes duplicates while keeping the original participant order.
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
            // The leaving user gets a separate event so the frontend can remove the conversation locally.
            messagingTemplate.convertAndSend(
                    "/topic/users/" + participant.getId() + "/conversations",
                    event
            );
        }

        sendConversationUpdatedEvent(savedConversation);
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

    public ConversationResponse updateGroupName(
            String currentLogin,
            String conversationId,
            UpdateGroupNameRequest request
    ) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only the group owner can rename the group.
        validateGroupOwnership(currentUser, conversation);

        String trimmedName = request.getName().trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }

        conversation.setName(trimmedName);
        Conversation updatedConversation = conversationRepository.save(conversation);

        sendConversationUpdatedEvent(updatedConversation);

        return mapToResponse(updatedConversation);
    }

    public ConversationResponse addParticipantToGroup(
            String currentLogin,
            String conversationId,
            UpdateGroupParticipantRequest request
    ) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only the group owner can rename the group.
        validateGroupOwnership(currentUser, conversation);

        User userToAdd = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User to add not found"));

        if (conversation.getParticipantIds().contains(userToAdd.getId())) {
            throw new IllegalArgumentException("User is already a participant of this group");
        }

        conversation.getParticipantIds().add(userToAdd.getId());
        Conversation updatedConversation = conversationRepository.save(conversation);

        sendConversationUpdatedEvent(updatedConversation);

        return mapToResponse(updatedConversation);
    }

    public ConversationResponse removeParticipantFromGroup(
            String currentLogin,
            String conversationId,
            UpdateGroupParticipantRequest request
    ) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only the group owner can rename the group.
        validateGroupOwnership(currentUser, conversation);

        String userIdToRemove = request.getUserId();

        if (!conversation.getParticipantIds().contains(userIdToRemove)) {
            throw new IllegalArgumentException("User is not a participant of this group");
        }

        if (userIdToRemove.equals(conversation.getOwnerId())) {
            throw new IllegalArgumentException("Owner cannot be removed from the group");
        }

        if (conversation.getParticipantIds().size() <= 3) {
            throw new IllegalArgumentException("Group must have at least 3 participants");
        }

        conversation.getParticipantIds().remove(userIdToRemove);
        Conversation updatedConversation = conversationRepository.save(conversation);

        sendConversationUpdatedEvent(updatedConversation);

        return mapToResponse(updatedConversation);
    }

    public void leaveGroup(String currentLogin, String conversationId) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        validateGroupParticipation(currentUser, conversation);

        // The owner cannot leave directly because the group must always have an owner.
        if (currentUser.getId().equals(conversation.getOwnerId())) {
            throw new IllegalArgumentException("Group owner must transfer ownership before leaving");
        }

        conversation.getParticipantIds().remove(currentUser.getId());

        Conversation updatedConversation = conversationRepository.save(conversation);

        sendConversationUpdatedEvent(updatedConversation);

        // The leaving user gets a separate event so the frontend can remove the conversation locally.
        messagingTemplate.convertAndSend(
                "/topic/users/" + currentUser.getId() + "/conversation-deleted",
                new ConversationDeletedEvent(conversationId)
        );
    }

    public ConversationResponse transferGroupOwnership(
            String currentLogin,
            String conversationId,
            TransferGroupOwnershipRequest request
    ) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only the group owner can rename the group.
        validateGroupOwnership(currentUser, conversation);

        String newOwnerId = request.getNewOwnerId();

        // Ownership can only be transferred to an existing group participant.
        if (!conversation.getParticipantIds().contains(newOwnerId)) {
            throw new IllegalArgumentException("New owner must be a participant of the group");
        }

        if (newOwnerId.equals(conversation.getOwnerId())) {
            throw new IllegalArgumentException("Selected user is already the owner");
        }

        conversation.setOwnerId(newOwnerId);
        Conversation updatedConversation = conversationRepository.save(conversation);

        sendConversationUpdatedEvent(updatedConversation);

        return mapToResponse(updatedConversation);
    }

    public void deleteGroup(String currentLogin, String conversationId) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only the group owner can rename the group.
        validateGroupOwnership(currentUser, conversation);

        // Participants are notified before the conversation and its messages are removed.
        for (String participantId : conversation.getParticipantIds()) {
            // The leaving user gets a separate event so the frontend can remove the conversation locally.
            messagingTemplate.convertAndSend(
                    "/topic/users/" + participantId + "/conversation-deleted",
                    new ConversationDeletedEvent(conversation.getId())
            );
        }

        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    // Ensures that group-only actions are not executed on private conversations.
    private void validateGroupParticipation(User currentUser, Conversation conversation) {
        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalArgumentException("This operation is only allowed for group conversations");
        }

        if (!conversation.getParticipantIds().contains(currentUser.getId())) {
            throw new IllegalArgumentException("You are not a participant of this group");
        }
    }

    // Ensures that only the current group owner can perform management actions.
    private void validateGroupOwnership(User currentUser, Conversation conversation) {
        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalArgumentException("This operation is only allowed for group conversations");
        }

        if (!currentUser.getId().equals(conversation.getOwnerId())) {
            throw new IllegalArgumentException("Only group owner can manage this group");
        }
    }

    // Sends a lightweight update event to every participant of the conversation.
    private void sendConversationUpdatedEvent(Conversation conversation) {
        ConversationUpdatedEvent event = new ConversationUpdatedEvent(
                conversation.getId(),
                conversation.getType().name(),
                conversation.getName()
        );

        // Participants are notified before the conversation and its messages are removed.
        for (String participantId : conversation.getParticipantIds()) {
            // The leaving user gets a separate event so the frontend can remove the conversation locally.
            messagingTemplate.convertAndSend(
                    "/topic/users/" + participantId + "/conversation-updates",
                    event
            );
        }
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