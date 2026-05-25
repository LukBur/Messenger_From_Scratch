import { ConversationResponse } from "@/types/conversation";
import { MessageResponse } from "@/types/message";
import { UserResponse } from "@/types/user";

// For private conversations, returns the participant that is not the current user.
export function getOtherParticipant(
  conversation: ConversationResponse,
  currentUser: UserResponse,
) {
  return (
    conversation.participants.find(
      (participant) => participant.id !== currentUser.id,
    ) || conversation.participants[0]
  );
}

// Merges messages from API polling and WebSocket events while preventing duplicates.
export function mergeMessages(
  currentMessages: MessageResponse[],
  incomingMessages: MessageResponse[],
) {
  const map = new Map<string, MessageResponse>();

  [...currentMessages, ...incomingMessages].forEach((message) => {
    map.set(message.id, message);
  });

  return Array.from(map.values()).sort((a, b) => {
    return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
  });
}
