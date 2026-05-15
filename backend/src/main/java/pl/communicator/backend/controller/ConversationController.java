package pl.communicator.backend.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.communicator.backend.dto.ConversationResponse;
import pl.communicator.backend.dto.CreateGroupConversationRequest;
import pl.communicator.backend.dto.CreatePrivateConversationRequest;
import pl.communicator.backend.service.ConversationService;

import java.util.List;

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
            Authentication authentication
    ) {
        // The authenticated username is taken from the JWT processed by Spring Security.
        return conversationService.createOrGetPrivateConversation(authentication.getName(), request);
    }

    @PostMapping("/group")
    public ConversationResponse createGroupConversation(
            @Valid @RequestBody CreateGroupConversationRequest request,
            Authentication authentication
    ) {
        // Returns all conversations that belong to the currently authenticated user.
        return conversationService.createGroupConversation(authentication.getName(), request);
    }

    @GetMapping("/my")
    public List<ConversationResponse> getMyConversations(Authentication authentication) {
        return conversationService.getMyConversations(authentication.getName());
    }
}