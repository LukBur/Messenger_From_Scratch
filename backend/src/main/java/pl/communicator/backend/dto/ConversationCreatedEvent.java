package pl.communicator.backend.dto;

import lombok.Getter;

@Getter
public class ConversationCreatedEvent {

    private String conversationId;
    private String type;

    public ConversationCreatedEvent() {
    }

    public ConversationCreatedEvent(String conversationId, String type) {
        this.conversationId = conversationId;
        this.type = type;
    }

}