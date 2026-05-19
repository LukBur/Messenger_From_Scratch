package pl.communicator.backend.repository;

import pl.communicator.backend.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    List<Message> findByDisappearingTrueAndExpiresAtBefore(Instant instant);
    void deleteByConversationId(String conversationId);
}