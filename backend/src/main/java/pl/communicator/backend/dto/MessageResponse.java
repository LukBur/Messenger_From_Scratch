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

    public MessageResponse() {
    }

    public MessageResponse(String id, String conversationId, String content, Instant createdAt,
                           boolean edited, Instant editedAt, MessageSenderResponse sender) {
        this.id = id;
        this.conversationId = conversationId;
        this.content = content;
        this.createdAt = createdAt;
        this.edited = edited;
        this.editedAt = editedAt;
        this.sender = sender;
    }

}