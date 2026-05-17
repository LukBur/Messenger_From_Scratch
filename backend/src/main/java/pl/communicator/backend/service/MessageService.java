package pl.communicator.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import pl.communicator.backend.dto.EditMessageRequest;
import pl.communicator.backend.dto.MessageResponse;
import pl.communicator.backend.dto.MessageSenderResponse;
import pl.communicator.backend.dto.SendMessageRequest;
import pl.communicator.backend.exception.ResourceNotFoundException;
import pl.communicator.backend.model.Conversation;
import pl.communicator.backend.model.Message;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.ConversationRepository;
import pl.communicator.backend.repository.MessageRepository;
import pl.communicator.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Sends a message only if the current user belongs to the selected conversation.
    public MessageResponse sendMessage(String currentLogin, SendMessageRequest request) {
        Integer disappearAfterSeconds = request.getDisappearAfterSeconds();

        boolean disappearing = disappearAfterSeconds != null
                && disappearAfterSeconds > 0;

        Instant expiresAt = disappearing
                ? Instant.now().plusSeconds(disappearAfterSeconds)
                : null;

        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantIds().contains(currentUser.getId())) {
            throw new IllegalArgumentException("You are not a participant of this conversation");
        }

        // Trimming prevents sending messages that contain only whitespace.
        String trimmedContent = request.getContent().trim();
        if (trimmedContent.isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(currentUser.getId());
        message.setContent(trimmedContent);
        message.setCreatedAt(Instant.now());
        message.setEdited(false);
        message.setEditedAt(null);

        message.setDisappearing(disappearing);
        message.setExpiresAt(expiresAt);

        Message savedMessage = messageRepository.save(message);

        // The conversation is updated so it can be sorted and previewed by the latest message.
        conversation.setLastActivityAt(savedMessage.getCreatedAt());
        conversation.setLastMessageId(savedMessage.getId());
        conversationRepository.save(conversation);

        MessageResponse response = mapToResponse(savedMessage);

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversation.getId(),
                response
        );

        return response;
    }

    public MessageResponse editMessage(String currentLogin, String messageId, EditMessageRequest request) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSenderId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You can only edit your own messages");
        }

        String trimmedContent = request.getContent().trim();
        if (trimmedContent.isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        message.setContent(trimmedContent);
        message.setEdited(true);
        message.setEditedAt(Instant.now());

        Message updatedMessage = messageRepository.save(message);
        MessageResponse response = mapToResponse(updatedMessage);

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + updatedMessage.getConversationId(),
                response
        );

        return response;
    }

    // Returns messages from a conversation after checking that the user has access to it.
    public List<MessageResponse> getConversationMessages(String currentLogin, String conversationId) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantIds().contains(currentUser.getId())) {
            throw new IllegalArgumentException("You are not a participant of this conversation");
        }

        List<Message> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        messages.stream()
                .filter(this::isExpired)
                .forEach(messageRepository::delete);

        return messages.stream()
                .filter(message -> !isExpired(message))
                .map(this::mapToResponse)
                .toList();
    }

    // Converts a message entity into a response object with sender information.
    private MessageResponse mapToResponse(Message message) {
        User sender = userRepository.findById(message.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        MessageSenderResponse senderResponse = new MessageSenderResponse(
                sender.getId(),
                sender.getLogin(),
                sender.getDisplayName(),
                sender.getAvatarUrl()
        );

        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getContent(),
                message.getCreatedAt(),
                message.isEdited(),
                message.getEditedAt(),
                message.getExpiresAt(),
                message.isDisappearing(),
                senderResponse
        );
    }

    private boolean isExpired(Message message) {
        return message.isDisappearing()
                && message.getExpiresAt() != null
                && Instant.now().isAfter(message.getExpiresAt());
    }
}