package pl.communicator.backend.dto;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class ConversationResponse {

    private String id;
    private String type;
    private String name;
    private String ownerId;
    private List<ConversationParticipantResponse> participants;
    private String createdBy;
    private Instant createdAt;
    private Instant lastActivityAt;
    private ConversationLastMessageResponse lastMessage;

    public ConversationResponse() {
    }

    public ConversationResponse(String id,
                                String type,
                                String name,
                                String ownerId,
                                List<ConversationParticipantResponse> participants,
                                String createdBy,
                                Instant createdAt,
                                Instant lastActivityAt,
                                ConversationLastMessageResponse lastMessage) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.ownerId = ownerId;
        this.participants = participants;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
        this.lastMessage = lastMessage;
    }

}