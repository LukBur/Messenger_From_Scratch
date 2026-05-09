"use client";

import { useEffect, useState } from "react";
import AuthForm from "@/components/AuthForm";
import ChatLayout from "@/components/ChatLayout";
import {
  createPrivateConversation,
  getCurrentUser,
  getMyConversations,
  loginUser,
  registerUser,
  searchUsers,
} from "@/lib/api";
import { ConversationResponse } from "@/types/conversation";
import { UserSearchResponse, UserResponse } from "@/types/user";

type AuthMode = "login" | "register";

export default function HomePage() {
  const [mode, setMode] = useState<AuthMode>("login");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingUser, setLoadingUser] = useState(true);

  const [currentUser, setCurrentUser] = useState<UserResponse | null>(null);
  const [searchResults, setSearchResults] = useState<UserSearchResponse[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [conversations, setConversations] = useState<ConversationResponse[]>([]);
  const [selectedConversation, setSelectedConversation] =
    useState<ConversationResponse | null>(null);

  const loadConversations = async (
    token: string,
    preferredConversationId?: string
  ) => {
    const data = await getMyConversations(token);
    setConversations(data);

    if (data.length === 0) {
      setSelectedConversation(null);
      return;
    }

    const targetId = preferredConversationId || selectedConversation?.id;
    const matchedConversation = data.find((item) => item.id === targetId);

    if (matchedConversation) {
      setSelectedConversation(matchedConversation);
    } else {
      setSelectedConversation(data[0]);
    }
  };

  const refreshConversations = async () => {
    const token = localStorage.getItem("token");
    if (!token) return;

    await loadConversations(token);
  };

  const fetchCurrentUser = async (token: string) => {
    try {
      setLoadingUser(true);
      const user = await getCurrentUser(token);
      setCurrentUser(user);
      await loadConversations(token);
      setMessage("");
    } catch (error) {
      localStorage.removeItem("token");
      setCurrentUser(null);
      setConversations([]);
      setSelectedConversation(null);
      setMessage(
        error instanceof Error ? error.message : "Could not load user data"
      );
    } finally {
      setLoadingUser(false);
    }
  };

  const handleLogin = async (payload: {
    login: string;
    password: string;
  }) => {
    try {
      setLoading(true);
      setMessage("");

      const data = await loginUser(payload);
      localStorage.setItem("token", data.token);

      await fetchCurrentUser(data.token);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Login failed");
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (payload: {
    email: string;
    login: string;
    displayName: string;
    password: string;
  }) => {
    try {
      setLoading(true);
      setMessage("");

      const data = await registerUser(payload);
      setMessage(data.message || "Registration successful");
      setMode("login");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  const handleSearchUsers = async (query: string) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setSearchLoading(true);
      const users = await searchUsers(token, query);
      setSearchResults(users);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Search failed");
    } finally {
      setSearchLoading(false);
    }
  };

  const handleStartConversation = async (userId: string) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");
      const conversation = await createPrivateConversation(token, userId);
      await loadConversations(token, conversation.id);
      setSearchResults([]);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not create conversation"
      );
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    setCurrentUser(null);
    setSearchResults([]);
    setConversations([]);
    setSelectedConversation(null);
    setMessage("Logged out");
    setMode("login");
  };

  useEffect(() => {
    const savedToken = localStorage.getItem("token");

    if (!savedToken) {
      setLoadingUser(false);
      return;
    }

    fetchCurrentUser(savedToken);
  }, []);

  if (loadingUser) {
    return (
      <main className="page-wrapper">
        <div className="center-box">
          <p>Loading...</p>
        </div>
      </main>
    );
  }

  return (
    <main className="page-wrapper">
      <div className="page-container">
        {currentUser ? (
          <>
            <ChatLayout
              currentUser={currentUser}
              searchResults={searchResults}
              searchLoading={searchLoading}
              conversations={conversations}
              selectedConversation={selectedConversation}
              onSearchUsers={handleSearchUsers}
              onStartConversation={handleStartConversation}
              onSelectConversation={setSelectedConversation}
              onRefreshConversations={refreshConversations}
              onLogout={handleLogout}
            />
            {message && <p className="status-message">{message}</p>}
          </>
        ) : (
          <AuthForm
            mode={mode}
            setMode={setMode}
            onLogin={handleLogin}
            onRegister={handleRegister}
            loading={loading}
            message={message}
          />
        )}
      </div>
    </main>
  );
}