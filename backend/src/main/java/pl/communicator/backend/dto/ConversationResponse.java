package pl.communicator.backend.dto;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class ConversationResponse {

    private String id;
    private String type;
    private List<ConversationParticipantResponse> participants;
    private String createdBy;
    private Instant createdAt;
    private Instant lastActivityAt;
    private ConversationLastMessageResponse lastMessage;

    public ConversationResponse() {
    }

    public ConversationResponse(String id, String type, List<ConversationParticipantResponse> participants,
                                String createdBy, Instant createdAt, Instant lastActivityAt,
                                ConversationLastMessageResponse lastMessage) {
        this.id = id;
        this.type = type;
        this.participants = participants;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
        this.lastMessage = lastMessage;
    }

}