package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter



public class SendMessageRequest {

    @NotBlank(message = "Conversation id is required")
    private String conversationId;

    @NotBlank(message = "Message content is required")
    private String content;

    private Integer disappearAfterSeconds;

    public SendMessageRequest() {
    }

}