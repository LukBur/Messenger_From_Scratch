"use client";

import { ConversationResponse } from "@/types/conversation";
import { UserResponse } from "@/types/user";

type ConversationListProps = {
  conversations: ConversationResponse[];
  currentUser: UserResponse;
  selectedConversationId: string | null;
  onSelectConversation: (conversation: ConversationResponse) => void;
};

function getConversationTitle(
  conversation: ConversationResponse,
  currentUser: UserResponse,
) {
  if (conversation.type === "GROUP") {
    return conversation.name || "Group conversation";
  }

  const otherParticipant =
    conversation.participants.find(
      (participant) => participant.id !== currentUser.id,
    ) || conversation.participants[0];

  return otherParticipant?.displayName || "Conversation";
}

function getConversationSubtitle(
  conversation: ConversationResponse,
  currentUser: UserResponse,
) {
  if (conversation.type === "GROUP") {
    return `${conversation.participants.length} members`;
  }

  const otherParticipant =
    conversation.participants.find(
      (participant) => participant.id !== currentUser.id,
    ) || conversation.participants[0];

  return otherParticipant ? `@${otherParticipant.login}` : "";
}

function getConversationAvatar(
  conversation: ConversationResponse,
  currentUser: UserResponse,
) {
  if (conversation.type === "GROUP") {
    return null;
  }

  const otherParticipant =
    conversation.participants.find(
      (participant) => participant.id !== currentUser.id,
    ) || conversation.participants[0];

  return otherParticipant?.avatarUrl || null;
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
            const title = getConversationTitle(conversation, currentUser);
            const subtitle = getConversationSubtitle(conversation, currentUser);
            const avatarUrl = getConversationAvatar(conversation, currentUser);

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
                <div className="conversation-item-content">
                  <div className="conversation-avatar">
                    {conversation.type === "GROUP" ? (
                      <span>G</span>
                    ) : avatarUrl ? (
                      <img src={avatarUrl} alt={title} />
                    ) : (
                      <span>{title.charAt(0).toUpperCase()}</span>
                    )}
                  </div>

                  <div className="conversation-item-body">
                    <div className="conversation-item-top">
                      <strong>{title}</strong>
                      <span className="conversation-login">{subtitle}</span>
                    </div>

                    <p className="conversation-preview">
                      {conversation.lastMessage
                        ? `${conversation.lastMessage.senderDisplayName}: ${conversation.lastMessage.content}`
                        : "No messages yet"}
                    </p>
                  </div>
                </div>
              </button>
            );
          })
        )}
      </div>
    </section>
  );
}
