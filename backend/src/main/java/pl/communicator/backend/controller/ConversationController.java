package pl.communicator.backend.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.communicator.backend.dto.*;
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
        return conversationService.createGroupConversation(authentication.getName(), request);
    }

    @PutMapping("/{conversationId}/group/name")
    public ConversationResponse updateGroupName(
            @PathVariable String conversationId,
            @Valid @RequestBody UpdateGroupNameRequest request,
            Authentication authentication
    ) {
        return conversationService.updateGroupName(authentication.getName(), conversationId, request);
    }

    @PostMapping("/{conversationId}/group/participants")
    public ConversationResponse addParticipantToGroup(
            @PathVariable String conversationId,
            @Valid @RequestBody UpdateGroupParticipantRequest request,
            Authentication authentication
    ) {
        return conversationService.addParticipantToGroup(authentication.getName(), conversationId, request);
    }

    @DeleteMapping("/{conversationId}/group/participants")
    public ConversationResponse removeParticipantFromGroup(
            @PathVariable String conversationId,
            @Valid @RequestBody UpdateGroupParticipantRequest request,
            Authentication authentication
    ) {
        return conversationService.removeParticipantFromGroup(authentication.getName(), conversationId, request);
    }

    @PostMapping("/{conversationId}/group/leave")
    public void leaveGroup(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        conversationService.leaveGroup(authentication.getName(), conversationId);
    }

    @PutMapping("/{conversationId}/group/owner")
    public ConversationResponse transferGroupOwnership(
            @PathVariable String conversationId,
            @Valid @RequestBody TransferGroupOwnershipRequest request,
            Authentication authentication
    ) {
        return conversationService.transferGroupOwnership(authentication.getName(), conversationId, request);
    }

    @DeleteMapping("/{conversationId}")
    public void deleteGroup(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        conversationService.deleteGroup(authentication.getName(), conversationId);
    }

    // Returns all conversations that belong to the currently authenticated user.
    @GetMapping("/my")
    public List<ConversationResponse> getMyConversations(Authentication authentication) {
        return conversationService.getMyConversations(authentication.getName());
    }
}