"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { StompSubscription } from "@stomp/stompjs";
import { subscribeToConversation } from "@/lib/websocket";
import { ConversationResponse } from "@/types/conversation";
import { UserResponse } from "@/types/user";
import { MessageResponse } from "@/types/message";
import { editMessage, getConversationMessages, sendMessage } from "@/lib/api";
import MessageList from "@/components/MessageList";
import MessageComposer from "@/components/MessageComposer";

type ConversationViewProps = {
  conversation: ConversationResponse | null;
  currentUser: UserResponse;
  onConversationUpdated: () => Promise<void>;
  onOpenManageGroup: () => void;
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

function mergeMessages(
  currentMessages: MessageResponse[],
  incomingMessages: MessageResponse[]
) {
  const map = new Map<string, MessageResponse>();

  [...currentMessages, ...incomingMessages].forEach((message) => {
    map.set(message.id, message);
  });

  return Array.from(map.values()).sort((a, b) => {
    return (
      new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );
  });
}

export default function ConversationView({
  conversation,
  currentUser,
  onConversationUpdated,
  onOpenManageGroup,
}: ConversationViewProps) {
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sendingMessage, setSendingMessage] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const subscriptionRef = useRef<StompSubscription | null>(null);
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);

  const activeConversationId = conversation?.id ?? null;

  const loadMessages = async (
    conversationId: string,
    options?: { silent?: boolean },
  ) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      if (!options?.silent) {
        setLoadingMessages(true);
      }

      setErrorMessage("");
      const data = await getConversationMessages(token, conversationId);

      setMessages((prev) => mergeMessages(prev, data));
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Could not load messages",
      );
    } finally {
      if (!options?.silent) {
        setLoadingMessages(false);
      }
    }
  };

  const handleSendMessage = async (
    content: string,
    disappearAfterSeconds?: number,
  ) => {
    if (!conversation) return;

    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setSendingMessage(true);
      setErrorMessage("");

      const sentMessage = await sendMessage(token, {
        conversationId: conversation.id,
        content,
        disappearAfterSeconds,
      });

      setMessages((prev) => mergeMessages(prev, [sentMessage]));
      await onConversationUpdated();
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Could not send message",
      );
    } finally {
      setSendingMessage(false);
    }
  };

  const handleEditMessage = async (messageId: string, content: string) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setErrorMessage("");

      const updatedMessage = await editMessage(token, messageId, content);

      setMessages((prev) =>
        prev.map((message) =>
          message.id === updatedMessage.id ? updatedMessage : message,
        ),
      );

      await onConversationUpdated();
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Could not edit message",
      );
    }
  };

  useEffect(() => {
    if (!conversation) {
      setMessages([]);
      return;
    }

    setMessages([]);
    void loadMessages(conversation.id);
  }, [conversation?.id]);

  useEffect(() => {
    if (!conversation) {
      return;
    }

    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    let isCancelled = false;

    const setupSubscription = async () => {
      const subscription = await subscribeToConversation(
        conversation.id,
        (incomingMessage) => {
          setMessages((prev) => mergeMessages(prev, [incomingMessage]));
          void onConversationUpdated();
        },
      );

      if (!isCancelled) {
        subscriptionRef.current = subscription;
      } else {
        subscription?.unsubscribe();
      }
    };

    void setupSubscription();

    return () => {
      isCancelled = true;

      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
    };
  }, [conversation?.id, onConversationUpdated]);

  useEffect(() => {
    if (!activeConversationId) return;

    const interval = setInterval(() => {
      void loadMessages(activeConversationId, { silent: true });
    }, 4000);

    return () => clearInterval(interval);
  }, [activeConversationId]);

  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;

    container.scrollTo({
      top: container.scrollHeight,
      behavior: "smooth",
    });
  }, [messages]);

  if (!conversation) {
    return (
      <section className="content-card">
        <p className="muted-text">
          Select a conversation or start a new one from the search panel.
        </p>
      </section>
    );
  }

  const otherParticipant =
    conversation.type === "PRIVATE"
      ? getOtherParticipant(conversation, currentUser)
      : null;

  return (
    <section className="content-card conversation-view-card">
      <div className="conversation-header">
        <div className="conversation-header-main">
          <div>
            <p className="eyebrow">
              {conversation.type === "GROUP"
                ? "Group conversation"
                : "Private conversation"}
            </p>

            <h2>
              {conversation.type === "GROUP"
                ? conversation.name || "Group conversation"
                : otherParticipant?.displayName || "Conversation"}
            </h2>

            <p className="muted-text">
              {conversation.type === "GROUP"
                ? `${conversation.participants.length} members`
                : `@${otherParticipant?.login}`}
            </p>
          </div>

          {conversation.type === "GROUP" && (
            <button
              type="button"
              className="secondary-button"
              onClick={onOpenManageGroup}
            >
              Manage group
            </button>
          )}
        </div>
      </div>

      <div className="conversation-messages-area" ref={messagesContainerRef}>
        {loadingMessages ? (
          <p className="muted-text">Loading messages...</p>
        ) : (
          <>
            <MessageList
              messages={messages}
              currentUser={currentUser}
              onEditMessage={handleEditMessage}
            />
          </>
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