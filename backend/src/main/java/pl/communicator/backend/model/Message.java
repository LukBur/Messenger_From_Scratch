package pl.communicator.backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    // Id of the conversation that this message belongs to.
    @Setter
    private String conversationId;

    // Id of the user who sent the message.
    @Setter
    private String senderId;

    @Setter
    private String content;

    @Setter
    private Instant createdAt;

    // Indicates whether the message content was changed after sending.
    @Setter
    private boolean edited;

    // Stores the time of the last edit, if the message was modified.
    @Setter
    private Instant editedAt;

    @Setter
    private Instant expiresAt;

    @Setter
    private boolean disappearing;

    public Message() {
    }

    public Message(String id,
                   String conversationId,
                   String senderId,
                   String content,
                   Instant createdAt,
                   boolean edited,
                   Instant editedAt,
                   Instant expiresAt,
                   boolean disappearing) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.edited = edited;
        this.editedAt = editedAt;
        this.expiresAt = expiresAt;
        this.disappearing = disappearing;
    }

}