package pl.communicator.backend.dto;

import lombok.Getter;

@Getter
public class ConversationParticipantResponse {

    private String id;
    private String login;
    private String displayName;
    private String avatarUrl;

    public ConversationParticipantResponse() {
    }

    public ConversationParticipantResponse(String id, String login, String displayName, String avatarUrl) {
        this.id = id;
        this.login = login;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }

}