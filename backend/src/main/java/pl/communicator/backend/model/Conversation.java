package pl.communicator.backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;


@Getter
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    @Setter
    private ConversationType type;
    @Setter
    private List<String> participantIds;
    @Setter
    private String createdBy;
    @Setter
    private Instant createdAt;

    public Conversation() {
    }

    public Conversation(String id, ConversationType type, List<String> participantIds, String createdBy, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.participantIds = participantIds;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

}