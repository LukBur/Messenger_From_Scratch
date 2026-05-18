package pl.communicator.backend.dto;

import lombok.Getter;

@Getter
public class ConversationUpdatedEvent {

    private String conversationId;
    private String type;
    private String name;

    public ConversationUpdatedEvent() {
    }

    public ConversationUpdatedEvent(String conversationId, String type, String name) {
        this.conversationId = conversationId;
        this.type = type;
        this.name = name;
    }

}