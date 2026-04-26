package pl.communicator.backend.controller;

import jakarta.validation.Valid;
import pl.communicator.backend.dto.ConversationResponse;
import pl.communicator.backend.dto.CreatePrivateConversationRequest;
import pl.communicator.backend.service.ConversationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    // Creates a private conversation or returns the existing one between the same users.
    @PostMapping("/private")
    public ConversationResponse createOrGetPrivateConversation(
            @Valid @RequestBody CreatePrivateConversationRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        // The authenticated username is taken from the JWT processed by Spring Security.
        return conversationService.createOrGetPrivateConversation(authentication.getName(), request);
    }

    // Returns all conversations that belong to the currently authenticated user.
    @GetMapping("/my")
    public java.util.List<ConversationResponse> getMyConversations(
            org.springframework.security.core.Authentication authentication
    ) {
        return conversationService.getMyConversations(authentication.getName());
    }
}