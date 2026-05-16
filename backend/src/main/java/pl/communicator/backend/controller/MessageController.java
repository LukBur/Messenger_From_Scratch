package pl.communicator.backend.controller;

import jakarta.validation.Valid;
import pl.communicator.backend.dto.EditMessageRequest;
import pl.communicator.backend.dto.MessageResponse;
import pl.communicator.backend.dto.SendMessageRequest;
import pl.communicator.backend.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // Sends a new message as the currently authenticated user.
    @PostMapping("/messages")
    public MessageResponse sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return messageService.sendMessage(authentication.getName(), request);
    }

    @PutMapping("/messages/{messageId}")
    public MessageResponse editMessage(
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request,
            Authentication authentication
    ) {
        return messageService.editMessage(authentication.getName(), messageId, request);
    }

    // Returns messages from a selected conversation, only for the authenticated user.
    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> getConversationMessages(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        return messageService.getConversationMessages(authentication.getName(), conversationId);
    }
}