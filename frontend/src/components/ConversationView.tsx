"use client";

import { useEffect, useState } from "react";
import { ConversationResponse } from "@/types/conversation";
import { UserResponse } from "@/types/user";
import { MessageResponse } from "@/types/message";
import { getConversationMessages, sendMessage } from "@/lib/api";
import MessageList from "@/components/MessageList";
import MessageComposer from "@/components/MessageComposer";

type ConversationViewProps = {
  conversation: ConversationResponse | null;
  currentUser: UserResponse;
  onConversationUpdated: () => Promise<void>;
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
  onConversationUpdated,
}: ConversationViewProps) {
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sendingMessage, setSendingMessage] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const loadMessages = async (conversationId: string) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setLoadingMessages(true);
      setErrorMessage("");
      const data = await getConversationMessages(token, conversationId);
      setMessages(data);
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Could not load messages"
      );
    } finally {
      setLoadingMessages(false);
    }
  };

  const handleSendMessage = async (content: string) => {
    if (!conversation) return;

    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setSendingMessage(true);
      setErrorMessage("");

      await sendMessage(token, {
        conversationId: conversation.id,
        content,
      });

      await loadMessages(conversation.id);
      await onConversationUpdated();
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Could not send message"
      );
    } finally {
      setSendingMessage(false);
    }
  };

  useEffect(() => {
    if (!conversation) {
      setMessages([]);
      return;
    }

    loadMessages(conversation.id);
  }, [conversation?.id]);

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
    <section className="content-card conversation-view-card">
      <div className="conversation-header">
        <div>
          <p className="eyebrow">Private conversation</p>
          <h2>{otherParticipant?.displayName || "Conversation"}</h2>
          <p className="muted-text">@{otherParticipant?.login}</p>
        </div>
      </div>

      <div className="conversation-messages-area">
        {loadingMessages ? (
          <p className="muted-text">Loading messages...</p>
        ) : (
          <MessageList messages={messages} currentUser={currentUser} />
        )}
      </div>

      {errorMessage && <p className="status-message">{errorMessage}</p>}

      <MessageComposer
        onSendMessage={handleSendMessage}
        loading={sendingMessage}
      />
    </section>
  );
}