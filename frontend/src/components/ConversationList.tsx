"use client";

import { ConversationResponse } from "@/types/conversation";
import { UserResponse } from "@/types/user";

type ConversationListProps = {
  conversations: ConversationResponse[];
  currentUser: UserResponse;
  selectedConversationId: string | null;
  onSelectConversation: (conversation: ConversationResponse) => void;
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

export default function ConversationList({
  conversations,
  currentUser,
  selectedConversationId,
  onSelectConversation,
}: ConversationListProps) {
  return (
    <section className="sidebar-card">
      <h2 className="section-title">My conversations</h2>

      <div className="conversation-list">
        {conversations.length === 0 ? (
          <p className="muted-text">No conversations yet.</p>
        ) : (
          conversations.map((conversation) => {
            const otherParticipant = getOtherParticipant(
              conversation,
              currentUser
            );

            return (
              <button
                key={conversation.id}
                type="button"
                className={
                  selectedConversationId === conversation.id
                    ? "conversation-item active"
                    : "conversation-item"
                }
                onClick={() => onSelectConversation(conversation)}
              >
                <div className="conversation-item-top">
                  <strong>{otherParticipant?.displayName || "Conversation"}</strong>
                  <span className="conversation-login">
                    @{otherParticipant?.login}
                  </span>
                </div>

                <p className="conversation-preview">
                  {conversation.lastMessage
                    ? `${conversation.lastMessage.senderDisplayName}: ${conversation.lastMessage.content}`
                    : "No messages yet"}
                </p>
              </button>
            );
          })
        )}
      </div>
    </section>
  );
}