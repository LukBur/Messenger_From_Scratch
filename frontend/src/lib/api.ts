import { LoginRequest, LoginResponse, ApiMessageResponse, RegisterRequest } from "@/types/auth";
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
  payload: RegisterRequest
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
  query: string
): Promise<UserSearchResponse[]> {
  const response = await fetch(
    `${API_URL}/users/search?query=${encodeURIComponent(query)}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  return handleJsonResponse<UserSearchResponse[]>(response);
}

export async function createPrivateConversation(
  token: string,
  targetUserId: string
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

export async function getMyConversations(
  token: string
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
  conversationId: string
): Promise<MessageResponse[]> {
  const response = await fetch(
    `${API_URL}/conversations/${conversationId}/messages`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  return handleJsonResponse<MessageResponse[]>(response);
}

export async function sendMessage(
  token: string,
  payload: SendMessageRequest
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