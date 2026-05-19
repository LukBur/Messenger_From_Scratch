export type ConversationParticipantResponse = {
  id: string;
  login: string;
  displayName: string;
  avatarUrl: string | null;
};

export type ConversationLastMessageResponse = {
  id: string;
  content: string;
  createdAt: string;
  edited: boolean;
  senderLogin: string;
  senderDisplayName: string;
};

export type ConversationResponse = {
  id: string;
  type: string;
  name: string | null;
  ownerId: string | null;
  participants: ConversationParticipantResponse[];
  createdBy: string;
  createdAt: string;
  lastActivityAt: string;
  lastMessage: ConversationLastMessageResponse | null;
};

export type ConversationCreatedEvent = {
  conversationId: string;
  type: string;
  name: string | null;
};

export type ConversationUpdatedEvent = {
  conversationId: string;
  type: string;
  name: string | null;
};

export type ConversationDeletedEvent = {
  conversationId: string;
};