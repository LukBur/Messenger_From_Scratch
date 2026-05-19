package pl.communicator.backend.dto;

import lombok.Getter;

@Getter
public class ConversationDeletedEvent {

    private String conversationId;

    public ConversationDeletedEvent() {
    }

    public ConversationDeletedEvent(String conversationId) {
        this.conversationId = conversationId;
    }

}