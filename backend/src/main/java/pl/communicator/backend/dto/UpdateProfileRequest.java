package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileRequest {

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String avatarUrl;

    public UpdateProfileRequest() {
    }

}