package pl.communicator.backend.dto;

import lombok.Getter;

@Getter
public class MessageDeletedEvent {

    private String messageId;
    private String conversationId;

    public MessageDeletedEvent() {
    }

    public MessageDeletedEvent(String messageId, String conversationId) {
        this.messageId = messageId;
        this.conversationId = conversationId;
    }

}