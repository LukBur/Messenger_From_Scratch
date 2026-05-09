import { ConversationResponse } from "@/types/conversation";
import { UserResponse } from "@/types/user";

type ConversationViewProps = {
  conversation: ConversationResponse | null;
  currentUser: UserResponse;
};

function getOtherParticipant(
  conversation: ConversationResponse,
  currentUser: UserResponse
) {
  return (
    conversation.participants.find(
      (participant) => participant.id !== currentUser.id
    ) || conversation.participants[0]
  );
}

export default function ConversationView({
  conversation,
  currentUser,
}: ConversationViewProps) {
  if (!conversation) {
    return (
      <section className="content-card">
        <p className="muted-text">
          Select a conversation or start a new one from the search panel.
        </p>
      </section>
    );
  }

  const otherParticipant = getOtherParticipant(conversation, currentUser);

  return (
    <section className="content-card">
      <div className="conversation-header">
        <div>
          <p className="eyebrow">Private conversation</p>
          <h2>{otherParticipant?.displayName || "Conversation"}</h2>
          <p className="muted-text">@{otherParticipant?.login}</p>
        </div>
      </div>

      <div className="conversation-placeholder">
        {conversation.lastMessage ? (
          <>
            <p className="muted-text">Last message</p>
            <div className="message-preview-card">
              <strong>{conversation.lastMessage.senderDisplayName}</strong>
              <p>{conversation.lastMessage.content}</p>
            </div>
          </>
        ) : (
          <p className="muted-text">
            No messages yet. Message view will be added next.
          </p>
        )}
      </div>
    </section>
  );
}