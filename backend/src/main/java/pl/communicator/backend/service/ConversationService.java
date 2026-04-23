package pl.communicator.backend.service;

import pl.communicator.backend.dto.ConversationParticipantResponse;
import pl.communicator.backend.dto.ConversationResponse;
import pl.communicator.backend.dto.CreatePrivateConversationRequest;
import pl.communicator.backend.exception.ResourceNotFoundException;
import pl.communicator.backend.model.Conversation;
import pl.communicator.backend.model.ConversationType;
import pl.communicator.backend.model.User;
import pl.communicator.backend.repository.ConversationRepository;
import pl.communicator.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    public ConversationResponse createOrGetPrivateConversation(String currentLogin,
                                                               CreatePrivateConversationRequest request) {
        User currentUser = userRepository.findByLogin(currentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("You cannot create a conversation with yourself");
        }

        List<Conversation> privateConversations = conversationRepository.findByType(ConversationType.PRIVATE);

        for (Conversation conversation : privateConversations) {
            List<String> participantIds = conversation.getParticipantIds();

            if (participantIds.size() == 2
                    && participantIds.contains(currentUser.getId())
                    && participantIds.contains(targetUser.getId())) {
                return mapToResponse(conversation);
            }
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.PRIVATE);
        conversation.setParticipantIds(List.of(currentUser.getId(), targetUser.getId()));
        conversation.setCreatedBy(currentUser.getId());
        conversation.setCreatedAt(Instant.now());

        Conversation savedConversation = conversationRepository.save(conversation);

        return mapToResponse(savedConversation);
    }

    private ConversationResponse mapToResponse(Conversation conversation) {
        List<ConversationParticipantResponse> participants = conversation.getParticipantIds().stream()
                .map(userId -> userRepository.findById(userId)
                        .map(user -> new ConversationParticipantResponse(
                                user.getId(),
                                user.getLogin(),
                                user.getDisplayName(),
                                user.getAvatarUrl()
                        ))
                        .orElseThrow(() -> new ResourceNotFoundException("Participant not found")))
                .toList();

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType().name(),
                participants,
                conversation.getCreatedBy(),
                conversation.getCreatedAt()
        );
    }
}