package pl.communicator.backend.dto;

import lombok.Getter;

@Getter
public class ConversationCreatedEvent {

    private String conversationId;
    private String type;
    private String name;

    public ConversationCreatedEvent() {
    }

    public ConversationCreatedEvent(String conversationId, String type, String name) {
        this.conversationId = conversationId;
        this.type = type;
        this.name = name;
    }

}