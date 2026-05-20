"use client";

import { useCallback, useEffect, useRef } from "react";
import { StompSubscription } from "@stomp/stompjs";
import { UserResponse } from "@/types/user";
import { ConversationResponse } from "@/types/conversation";
import {
  connectStompClient,
  disconnectStompClient,
  subscribeToConversationDeleted,
  subscribeToConversationManagementUpdates,
  subscribeToConversationUpdates,
} from "@/lib/websocket";

type UseUserWebSocketParams = {
  currentUser: UserResponse | null;
  refreshConversations: () => Promise<void>;
  setConversations: React.Dispatch<
    React.SetStateAction<ConversationResponse[]>
  >;
  setSelectedConversation: React.Dispatch<
    React.SetStateAction<ConversationResponse | null>
  >;
  closeManageGroup: () => void;
};

export function useUserWebSocket({
  currentUser,
  refreshConversations,
  setConversations,
  setSelectedConversation,
  closeManageGroup,
}: UseUserWebSocketParams) {
  const conversationUpdatesSubscriptionRef = useRef<StompSubscription | null>(
    null,
  );
  const conversationManagementSubscriptionRef =
    useRef<StompSubscription | null>(null);
  const conversationDeletedSubscriptionRef = useRef<StompSubscription | null>(
    null,
  );

  const clearUserSubscriptions = useCallback(() => {
    if (conversationUpdatesSubscriptionRef.current) {
      conversationUpdatesSubscriptionRef.current.unsubscribe();
      conversationUpdatesSubscriptionRef.current = null;
    }

    if (conversationManagementSubscriptionRef.current) {
      conversationManagementSubscriptionRef.current.unsubscribe();
      conversationManagementSubscriptionRef.current = null;
    }

    if (conversationDeletedSubscriptionRef.current) {
      conversationDeletedSubscriptionRef.current.unsubscribe();
      conversationDeletedSubscriptionRef.current = null;
    }
  }, []);

  useEffect(() => {
    if (!currentUser) return;

    connectStompClient(
      async () => {
        clearUserSubscriptions();

        const updatesSubscription = await subscribeToConversationUpdates(
          currentUser.id,
          () => {
            void refreshConversations();
          },
        );
        conversationUpdatesSubscriptionRef.current = updatesSubscription;

        const managementSubscription =
          await subscribeToConversationManagementUpdates(currentUser.id, () => {
            void refreshConversations();
          });
        conversationManagementSubscriptionRef.current = managementSubscription;

        const deletedSubscription = await subscribeToConversationDeleted(
          currentUser.id,
          (event) => {
            setConversations((prev) =>
              prev.filter(
                (conversation) => conversation.id !== event.conversationId,
              ),
            );

            setSelectedConversation((prev) =>
              prev?.id === event.conversationId ? null : prev,
            );

            closeManageGroup();
          },
        );
        conversationDeletedSubscriptionRef.current = deletedSubscription;
      },
      (error) => {
        console.error(error);
      },
    );

    return () => {
      clearUserSubscriptions();
      disconnectStompClient();
    };
  }, [
    currentUser,
    refreshConversations,
    setConversations,
    setSelectedConversation,
    closeManageGroup,
    clearUserSubscriptions,
  ]);

  return {
    clearUserSubscriptions,
  };
}
