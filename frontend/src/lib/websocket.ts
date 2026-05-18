import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { MessageResponse, MessageDeletedEvent } from "@/types/message";
import { ConversationCreatedEvent, ConversationUpdatedEvent } from "@/types/conversation";

let stompClient: Client | null = null;
let isDisconnecting = false;

export function createStompClient() {
  return new Client({
    webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
    reconnectDelay: 5000,
    debug: () => {},
  });
}

export function connectStompClient(
  onConnected?: () => void,
  onError?: (error: string) => void
) {
  if (stompClient) {
    if (stompClient.connected) {
      onConnected?.();
    }
    return stompClient;
  }

  isDisconnecting = false;
  stompClient = createStompClient();

  stompClient.onConnect = () => {
    onConnected?.();
  };

  stompClient.onStompError = (frame) => {
    if (isDisconnecting) return;
    onError?.(frame.headers["message"] || "WebSocket STOMP error");
  };

  stompClient.onWebSocketError = () => {
    if (isDisconnecting) return;
    onError?.("WebSocket connection error");
  };

  stompClient.onWebSocketClose = () => {
    if (isDisconnecting) return;
  };

  stompClient.activate();
  return stompClient;
}

export async function subscribeToConversation(
  conversationId: string,
  onMessage: (message: MessageResponse) => void
): Promise<StompSubscription | null> {
  if (!stompClient) {
    return null;
  }

  if (!stompClient.connected) {
    await waitForConnection(stompClient, 3000);
  }

  if (!stompClient.connected) {
    return null;
  }

  return stompClient.subscribe(
    `/topic/conversations/${conversationId}`,
    (frame: IMessage) => {
      const body = JSON.parse(frame.body) as MessageResponse;
      onMessage(body);
    }
  );
}

export async function subscribeToConversationUpdates(
  userId: string,
  onEvent: (event: ConversationCreatedEvent) => void
): Promise<StompSubscription | null> {
  if (!stompClient) {
    return null;
  }

  if (!stompClient.connected) {
    await waitForConnection(stompClient, 3000);
  }

  if (!stompClient.connected) {
    return null;
  }

  return stompClient.subscribe(
    `/topic/users/${userId}/conversations`,
    (frame: IMessage) => {
      const body = JSON.parse(frame.body) as ConversationCreatedEvent;
      onEvent(body);
    }
  );
}

export async function subscribeToConversationManagementUpdates(
  userId: string,
  onEvent: (event: ConversationUpdatedEvent) => void,
): Promise<StompSubscription | null> {
  if (!stompClient) {
    return null;
  }

  if (!stompClient.connected) {
    await waitForConnection(stompClient, 3000);
  }

  if (!stompClient.connected) {
    return null;
  }

  return stompClient.subscribe(
    `/topic/users/${userId}/conversation-updates`,
    (frame: IMessage) => {
      const body = JSON.parse(frame.body) as ConversationUpdatedEvent;
      onEvent(body);
    },
  );
}

export async function subscribeToDeletedMessages(
  conversationId: string,
  onDelete: (event: MessageDeletedEvent) => void,
): Promise<StompSubscription | null> {
  if (!stompClient) {
    return null;
  }

  if (!stompClient.connected) {
    await waitForConnection(stompClient, 3000);
  }

  if (!stompClient.connected) {
    return null;
  }

  return stompClient.subscribe(
    `/topic/conversations/${conversationId}/deleted`,
    (frame: IMessage) => {
      const body = JSON.parse(frame.body) as MessageDeletedEvent;
      onDelete(body);
    },
  );
}

export function disconnectStompClient() {
  if (stompClient) {
    isDisconnecting = true;
    stompClient.deactivate();
    stompClient = null;
  }
}

function waitForConnection(client: Client, timeoutMs: number): Promise<void> {
  return new Promise((resolve) => {
    const start = Date.now();

    const interval = setInterval(() => {
      if (client.connected) {
        clearInterval(interval);
        resolve();
        return;
      }

      if (Date.now() - start > timeoutMs) {
        clearInterval(interval);
        resolve();
      }
    }, 100);
  });
}