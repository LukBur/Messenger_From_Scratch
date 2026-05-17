export type MessageSenderResponse = {
  id: string;
  login: string;
  displayName: string;
  avatarUrl: string | null;
};

export type MessageResponse = {
  id: string;
  conversationId: string;
  content: string;
  createdAt: string;
  edited: boolean;
  editedAt: string | null;
  expiresAt: string | null;
  disappearing: boolean;
  sender: MessageSenderResponse;
};

export type SendMessageRequest = {
  conversationId: string;
  content: string;
  disappearAfterSeconds?: number;
};