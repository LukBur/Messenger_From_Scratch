package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EditMessageRequest {

    @NotBlank(message = "Message content is required")
    private String content;

    public EditMessageRequest() {
    }

}