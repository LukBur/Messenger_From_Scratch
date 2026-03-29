package pl.communicator.backend.dto;

import lombok.Getter;

@Getter
public class AuthResponse {

    private String message;

    public AuthResponse() {
    }

    public AuthResponse(String message) {
        this.message = message;
    }

}