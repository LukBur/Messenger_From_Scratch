package pl.communicator.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TransferGroupOwnershipRequest {

    @NotBlank(message = "New owner id is required")
    private String newOwnerId;

    public TransferGroupOwnershipRequest() {
    }

}