package pl.communicator.backend.controller;

import jakarta.validation.Valid;
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

    @PostMapping("/messages")
    public MessageResponse sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        return messageService.sendMessage(authentication.getName(), request);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> getConversationMessages(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        return messageService.getConversationMessages(authentication.getName(), conversationId);
    }
}