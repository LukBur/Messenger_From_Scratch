package pl.communicator.backend.service;

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

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    public MessageResponse sendMessage(String currentLogin, SendMessageRequest request) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantIds().contains(currentUser.getId())) {
            throw new IllegalArgumentException("You are not a participant of this conversation");
        }

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

        Message savedMessage = messageRepository.save(message);

        return mapToResponse(savedMessage);
    }

    public List<MessageResponse> getConversationMessages(String currentLogin, String conversationId) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantIds().contains(currentUser.getId())) {
            throw new IllegalArgumentException("You are not a participant of this conversation");
        }

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::mapToResponse)
                .toList();
    }

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
                senderResponse
        );
    }
}