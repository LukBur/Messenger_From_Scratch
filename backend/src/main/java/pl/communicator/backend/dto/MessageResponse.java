package pl.communicator.backend.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class MessageResponse {

    private String id;
    private String conversationId;
    private String content;
    private Instant createdAt;
    private boolean edited;
    private Instant editedAt;
    private MessageSenderResponse sender;
    private Instant expiresAt;
    private boolean disappearing;

    public MessageResponse() {
    }

    public MessageResponse(String id, String conversationId, String content, Instant createdAt,
                           boolean edited, Instant editedAt, Instant expiresAt, boolean disappearing, MessageSenderResponse sender) {
        this.id = id;
        this.conversationId = conversationId;
        this.content = content;
        this.createdAt = createdAt;
        this.edited = edited;
        this.editedAt = editedAt;
        this.expiresAt = expiresAt;
        this.disappearing = disappearing;
        this.sender = sender;
    }

}