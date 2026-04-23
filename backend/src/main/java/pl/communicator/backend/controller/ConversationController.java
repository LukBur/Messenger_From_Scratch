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

    @PostMapping("/private")
    public ConversationResponse createOrGetPrivateConversation(
            @Valid @RequestBody CreatePrivateConversationRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return conversationService.createOrGetPrivateConversation(authentication.getName(), request);
    }

    @GetMapping("/my")
    public java.util.List<ConversationResponse> getMyConversations(
            org.springframework.security.core.Authentication authentication
    ) {
        return conversationService.getMyConversations(authentication.getName());
    }
}