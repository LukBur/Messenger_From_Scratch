"use client";

import { useEffect, useRef, useState } from "react";
import { StompSubscription } from "@stomp/stompjs";
import {
  subscribeToConversation,
  subscribeToDeletedMessages,
} from "@/lib/websocket";
import { ConversationResponse } from "@/types/conversation";
import { UserResponse } from "@/types/user";
import { MessageResponse } from "@/types/message";
import {
  deleteMessage,
  editMessage,
  getConversationMessages,
  sendMessage,
} from "@/lib/api";
import MessageList from "@/components/MessageList";
import MessageComposer from "@/components/MessageComposer";
import {
  getOtherParticipant,
  mergeMessages,
} from "@/utils/conversationHelpers";

type ConversationViewProps = {
  conversation: ConversationResponse | null;
  currentUser: UserResponse;
  onConversationUpdated: () => Promise<void>;
  onOpenManageGroup: () => void;
};

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

  // Keeps active WebSocket subscriptions between renders so they can be cleaned up properly.
  const subscriptionRef = useRef<StompSubscription | null>(null);
  const deleteSubscriptionRef = useRef<StompSubscription | null>(null);
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);

  const activeConversationId = conversation?.id ?? null;

  // Loads conversation messages from the API with optional silent refresh mode.
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

      // Immediately updates local state after the API confirms message creation.
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

  const handleDeleteMessage = async (messageId: string) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setErrorMessage("");

      await deleteMessage(token, messageId);

      setMessages((prev) => prev.filter((message) => message.id !== messageId));
      await onConversationUpdated();
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Could not delete message",
      );
    }
  };

  // Resets local messages when switching between conversations.
  useEffect(() => {
    if (!conversation) {
      setMessages([]);
      return;
    }

    setMessages([]);
    void loadMessages(conversation.id);
  }, [conversation?.id]);

  // Subscribes to real-time incoming messages for the selected conversation.
  useEffect(() => {
    if (!conversation) {
      return;
    }

    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    // Prevents assigning subscriptions after the effect has already been cleaned up.
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

  // Subscribes to real-time message deletion events.
  useEffect(() => {
    if (!conversation) {
      return;
    }

    if (deleteSubscriptionRef.current) {
      deleteSubscriptionRef.current.unsubscribe();
      deleteSubscriptionRef.current = null;
    }

    let isCancelled = false;

    const setupDeleteSubscription = async () => {
      const subscription = await subscribeToDeletedMessages(
        conversation.id,
        (event) => {
          setMessages((prev) =>
            prev.filter((message) => message.id !== event.messageId),
          );

          void onConversationUpdated();
        },
      );

      if (!isCancelled) {
        deleteSubscriptionRef.current = subscription;
      } else {
        subscription?.unsubscribe();
      }
    };

    void setupDeleteSubscription();

    return () => {
      isCancelled = true;

      if (deleteSubscriptionRef.current) {
        deleteSubscriptionRef.current.unsubscribe();
        deleteSubscriptionRef.current = null;
      }
    };
  }, [conversation?.id, onConversationUpdated]);

  // Periodic refresh acts as a fallback in case a WebSocket event is missed.
  useEffect(() => {
    if (!activeConversationId) return;

    const interval = setInterval(() => {
      void loadMessages(activeConversationId, { silent: true });
    }, 4000);

    return () => clearInterval(interval);
  }, [activeConversationId]);

  // Automatically scrolls to the newest message whenever the message list changes.
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
        <p className="eyebrow">
          {conversation.type === "GROUP"
            ? "Group conversation"
            : "Private conversation"}
        </p>

        <div className="conversation-title-row">
          <h2>
            {conversation.type === "GROUP"
              ? conversation.name || "Group conversation"
              : otherParticipant?.displayName || "Conversation"}
          </h2>

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

        <p className="muted-text">
          {conversation.type === "GROUP"
            ? `${conversation.participants.length} members`
            : `@${otherParticipant?.login}`}
        </p>
      </div>

      <div className="conversation-messages-area" ref={messagesContainerRef}>
        {loadingMessages ? (
          <p className="muted-text">Loading messages...</p>
        ) : (
          <MessageList
            messages={messages}
            currentUser={currentUser}
            onEditMessage={handleEditMessage}
            onDeleteMessage={handleDeleteMessage}
          />
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
