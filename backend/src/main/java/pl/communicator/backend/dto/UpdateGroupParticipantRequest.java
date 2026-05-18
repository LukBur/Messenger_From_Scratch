package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateGroupParticipantRequest {

    @NotBlank(message = "User id is required")
    private String userId;

    public UpdateGroupParticipantRequest() {
    }

}