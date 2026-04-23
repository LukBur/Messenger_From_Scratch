package pl.communicator.backend.repository;

import pl.communicator.backend.model.Conversation;
import pl.communicator.backend.model.ConversationType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    List<Conversation> findByType(ConversationType type);
    List<Conversation> findByParticipantIdsContaining(String participantId);
}