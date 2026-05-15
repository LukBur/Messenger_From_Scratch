package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CreateGroupConversationRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    @NotEmpty(message = "Participant ids are required")
    private List<String> participantIds;

    public CreateGroupConversationRequest() {
    }

}