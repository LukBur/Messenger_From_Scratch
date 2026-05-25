package pl.communicator.backend.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.communicator.backend.dto.MessageDeletedEvent;
import pl.communicator.backend.model.Conversation;
import pl.communicator.backend.model.Message;
import pl.communicator.backend.repository.ConversationRepository;
import pl.communicator.backend.repository.MessageRepository;

import java.time.Instant;
import java.util.List;

@Service
public class MessageExpirationService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageExpirationService(MessageRepository messageRepository,
                                    ConversationRepository conversationRepository,
                                    SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Runs periodically to remove disappearing messages after their expiration time.
    @Scheduled(fixedRate = 1000)
    public void removeExpiredMessages() {
        List<Message> expiredMessages =
                messageRepository.findByDisappearingTrueAndExpiresAtBefore(Instant.now());

        for (Message message : expiredMessages) {
            String conversationId = message.getConversationId();
            String deletedMessageId = message.getId();

            messageRepository.delete(message);

            updateConversationAfterMessageDeletion(conversationId, deletedMessageId);

            // Notify clients so the deleted message can be removed from the conversation view in real time.
            messagingTemplate.convertAndSend(
                    "/topic/conversations/" + conversationId + "/deleted",
                    new MessageDeletedEvent(deletedMessageId, conversationId)
            );
        }
    }

    private void updateConversationAfterMessageDeletion(String conversationId, String deletedMessageId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return;
        }

        // If the deleted message was the latest one, the conversation preview must point to the new latest message.
        if (deletedMessageId.equals(conversation.getLastMessageId())) {
            List<Message> remainingMessages =
                    messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

            // When no messages are left, the conversation falls back to its creation time.
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
}