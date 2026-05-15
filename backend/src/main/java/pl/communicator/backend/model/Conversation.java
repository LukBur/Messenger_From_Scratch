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

    // Stores ids of users who are members of this conversation.
    @Setter
    private List<String> participantIds;

    @Setter
    private String name;

    @Setter
    private String ownerId;

    // Id of the user who created the conversation.
    @Setter
    private String createdBy;

    @Setter
    private Instant createdAt;

    // Used for sorting conversations by the newest activity.
    @Setter
    private Instant lastActivityAt;

    // Stores the id of the latest message for quick conversation previews.
    @Setter
    private String lastMessageId;

    public Conversation() {
    }

    public Conversation(String id, ConversationType type, List<String> participantIds, String createdBy,
                        Instant createdAt, Instant lastActivityAt, String lastMessageId) {
        this.id = id;
        this.type = type;
        this.participantIds = participantIds;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
        this.lastMessageId = lastMessageId;
    }

}