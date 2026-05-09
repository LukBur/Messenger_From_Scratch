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
  participants: ConversationParticipantResponse[];
  createdBy: string;
  createdAt: string;
  lastActivityAt: string;
  lastMessage: ConversationLastMessageResponse | null;
};