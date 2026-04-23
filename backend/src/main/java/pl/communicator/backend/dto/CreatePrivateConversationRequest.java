package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreatePrivateConversationRequest {

    @NotBlank(message = "Target user id is required")
    private String targetUserId;

    public CreatePrivateConversationRequest() {
    }

}