package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateGroupNameRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    public UpdateGroupNameRequest() {
    }

}