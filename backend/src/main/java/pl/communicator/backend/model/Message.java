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

    @Setter
    private String conversationId;
    @Setter
    private String senderId;
    @Setter
    private String content;
    @Setter
    private Instant createdAt;
    @Setter
    private boolean edited;
    @Setter
    private Instant editedAt;

    public Message() {
    }

    public Message(String id, String conversationId, String senderId, String content,
                   Instant createdAt, boolean edited, Instant editedAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.edited = edited;
        this.editedAt = editedAt;
    }

}