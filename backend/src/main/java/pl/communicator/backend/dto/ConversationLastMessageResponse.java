package pl.communicator.backend.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ConversationLastMessageResponse {

    private String id;
    private String content;
    private Instant createdAt;
    private boolean edited;
    private String senderLogin;
    private String senderDisplayName;

    public ConversationLastMessageResponse() {
    }

    public ConversationLastMessageResponse(String id, String content, Instant createdAt,
                                           boolean edited, String senderLogin, String senderDisplayName) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.edited = edited;
        this.senderLogin = senderLogin;
        this.senderDisplayName = senderDisplayName;
    }

}