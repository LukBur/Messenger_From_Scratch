import {
  LoginRequest,
  LoginResponse,
  ApiMessageResponse,
  RegisterRequest,
} from "@/types/auth";
import { UserSearchResponse, UserResponse } from "@/types/user";
import { ConversationResponse } from "@/types/conversation";
import { MessageResponse, SendMessageRequest } from "@/types/message";

const API_URL = "http://localhost:8080/api";

async function handleJsonResponse<T>(response: Response): Promise<T> {
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const errorMessage =
      data?.message || `Request failed with status ${response.status}`;
    throw new Error(errorMessage);
  }

  return data as T;
}

export async function loginUser(payload: LoginRequest): Promise<LoginResponse> {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return handleJsonResponse<LoginResponse>(response);
}

export async function registerUser(
  payload: RegisterRequest,
): Promise<ApiMessageResponse> {
  const response = await fetch(`${API_URL}/auth/register`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return handleJsonResponse<ApiMessageResponse>(response);
}

export async function getCurrentUser(token: string): Promise<UserResponse> {
  const response = await fetch(`${API_URL}/users/me`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  return handleJsonResponse<UserResponse>(response);
}

export async function searchUsers(
  token: string,
  query: string,
): Promise<UserSearchResponse[]> {
  const response = await fetch(
    `${API_URL}/users/search?query=${encodeURIComponent(query)}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  );

  return handleJsonResponse<UserSearchResponse[]>(response);
}

export async function updateProfile(
  token: string,
  payload: {
    displayName: string;
    avatarUrl: string | null;
  },
): Promise<UserResponse> {
  const response = await fetch(`${API_URL}/users/me/profile`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });

  return handleJsonResponse<UserResponse>(response);
}

export async function createPrivateConversation(
  token: string,
  targetUserId: string,
): Promise<ConversationResponse> {
  const response = await fetch(`${API_URL}/conversations/private`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      targetUserId,
    }),
  });

  return handleJsonResponse<ConversationResponse>(response);
}

export async function createGroupConversation(
  token: string,
  payload: {
    name: string;
    participantIds: string[];
  },
): Promise<ConversationResponse> {
  const response = await fetch(`${API_URL}/conversations/group`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });

  return handleJsonResponse<ConversationResponse>(response);
}

export async function updateGroupName(
  token: string,
  conversationId: string,
  name: string,
): Promise<ConversationResponse> {
  const response = await fetch(
    `${API_URL}/conversations/${conversationId}/group/name`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ name }),
    },
  );

  return handleJsonResponse<ConversationResponse>(response);
}

export async function addParticipantToGroup(
  token: string,
  conversationId: string,
  userId: string,
): Promise<ConversationResponse> {
  const response = await fetch(
    `${API_URL}/conversations/${conversationId}/group/participants`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ userId }),
    },
  );

  return handleJsonResponse<ConversationResponse>(response);
}

export async function removeParticipantFromGroup(
  token: string,
  conversationId: string,
  userId: string,
): Promise<ConversationResponse> {
  const response = await fetch(
    `${API_URL}/conversations/${conversationId}/group/participants`,
    {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ userId }),
    },
  );

  return handleJsonResponse<ConversationResponse>(response);
}

export async function getMyConversations(
  token: string,
): Promise<ConversationResponse[]> {
  const response = await fetch(`${API_URL}/conversations/my`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  return handleJsonResponse<ConversationResponse[]>(response);
}

export async function getConversationMessages(
  token: string,
  conversationId: string,
): Promise<MessageResponse[]> {
  const response = await fetch(
    `${API_URL}/conversations/${conversationId}/messages`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  );

  return handleJsonResponse<MessageResponse[]>(response);
}

export async function sendMessage(
  token: string,
  payload: SendMessageRequest,
): Promise<MessageResponse> {
  const response = await fetch(`${API_URL}/messages`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });

  return handleJsonResponse<MessageResponse>(response);
}

export async function editMessage(
  token: string,
  messageId: string,
  content: string,
): Promise<MessageResponse> {
  const response = await fetch(`${API_URL}/messages/${messageId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      content,
    }),
  });

  return handleJsonResponse<MessageResponse>(response);
}

export async function deleteMessage(
  token: string,
  messageId: string,
): Promise<void> {
  const response = await fetch(`${API_URL}/messages/${messageId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    const data = await response.json().catch(() => null);
    const errorMessage =
      data?.message || `Request failed with status ${response.status}`;
    throw new Error(errorMessage);
  }
}