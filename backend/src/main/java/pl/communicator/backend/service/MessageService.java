package pl.communicator.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import pl.communicator.backend.dto.*;
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

        // Disappearing messages receive an expiration timestamp based on the requested lifetime.
        boolean disappearing = disappearAfterSeconds != null
                && disappearAfterSeconds > 0;

        Instant expiresAt = disappearing
                ? Instant.now().plusSeconds(disappearAfterSeconds)
                : null;

        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only conversation participants are allowed to send messages to this conversation.
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

        // The new message is broadcast to all subscribed conversation clients in real time.
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

        // Expired disappearing messages cannot be edited anymore.
        if (isExpired(message)) {
            throw new IllegalArgumentException("Message has already expired");
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

        // Updated message data is pushed to connected clients so edits appear instantly.
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + updatedMessage.getConversationId(),
                response
        );

        return response;
    }

    public void deleteMessage(String currentLogin, String messageId) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        Conversation conversation = conversationRepository.findById(message.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isAuthor = message.getSenderId().equals(currentUser.getId());

        // Group owners are allowed to remove messages from their own groups.
        boolean isGroupOwner = conversation.getType().name().equals("GROUP")
                && currentUser.getId().equals(conversation.getOwnerId());

        if (!isAuthor && !isGroupOwner) {
            throw new IllegalArgumentException("You cannot delete this message");
        }

        String conversationId = message.getConversationId();
        String deletedMessageId = message.getId();

        messageRepository.delete(message);

        updateConversationAfterMessageDeletion(conversationId, deletedMessageId);

        // Connected clients are notified so the deleted message can be removed from the UI.
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId + "/deleted",
                new MessageDeletedEvent(deletedMessageId, conversationId)
        );
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

        // Expired disappearing messages are cleaned up before returning the conversation history.
        messages.stream()
                .filter(this::isExpired)
                .forEach(messageRepository::delete);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .filter(message -> !isExpired(message))
                .map(this::mapToResponse)
                .toList();
    }

    private void updateConversationAfterMessageDeletion(String conversationId, String deletedMessageId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return;
        }

        // If the deleted message was the latest one, the conversation preview must be recalculated.
        if (deletedMessageId.equals(conversation.getLastMessageId())) {
            List<Message> remainingMessages =
                    messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

            if (remainingMessages.isEmpty()) {
                conversation.setLastMessageId(null);
                conversation.setLastActivityAt(conversation.getCreatedAt());
            } else {
                Message lastMessage = remainingMessages.get(remainingMessages.size() - 1);
                conversation.setLastMessageId(lastMessage.getId());
                conversation.setLastActivityAt(lastMessage.getCreatedAt());
            }

            conversationRepository.save(conversation);
        }
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

    // Determines whether a disappearing message should already be treated as expired.
    private boolean isExpired(Message message) {
        return message.isDisappearing()
                && message.getExpiresAt() != null
                && Instant.now().isAfter(message.getExpiresAt());
    }
}