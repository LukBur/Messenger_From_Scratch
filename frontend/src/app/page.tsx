"use client";

import { useCallback, useEffect, useState } from "react";
import AuthForm from "@/components/AuthForm";
import ChatLayout from "@/components/ChatLayout";
import {
  changePassword,
  getCurrentUser,
  loginUser,
  registerUser,
  updateProfile,
} from "@/lib/api";
import { UserResponse } from "@/types/user";
import { useConversations } from "@/hooks/useConversations";
import { useUserWebSocket } from "@/hooks/useUserWebSocket";

type AuthMode = "login" | "register";

export default function HomePage() {
  // Stores authentication mode and global UI feedback for login/register actions.
  const [mode, setMode] = useState<AuthMode>("login");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingUser, setLoadingUser] = useState(true);

  const [isCreateGroupOpen, setIsCreateGroupOpen] = useState(false);
  const [isManageGroupOpen, setIsManageGroupOpen] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);

  const [currentUser, setCurrentUser] = useState<UserResponse | null>(null);

  const {
    searchResults,
    searchLoading,
    conversations,
    selectedConversation,
    setConversations,
    setSelectedConversation,
    setSearchResults,
    loadConversations,
    refreshConversations,
    handleSearchUsers,
    handleStartConversation,
    handleCreateGroup,
    handleUpdateGroupName,
    handleAddParticipant,
    handleRemoveParticipant,
    handleLeaveGroup,
    handleTransferOwnership,
    handleDeleteGroup,
  } = useConversations({
    setMessage,
    onCloseManageGroup: () => setIsManageGroupOpen(false),
    onCloseCreateGroup: () => setIsCreateGroupOpen(false),
  });

  const handleCloseCreateGroup = useCallback(() => {
    setIsCreateGroupOpen(false);
    setSearchResults([]);
  }, [setSearchResults]);

  const closeManageGroup = useCallback(() => {
    setIsManageGroupOpen(false);
  }, []);

  const { clearUserSubscriptions } = useUserWebSocket({
    currentUser,
    refreshConversations,
    setConversations,
    setSelectedConversation,
    closeManageGroup,
  });

  // Loads the authenticated user and their conversations using the stored JWT token.
  const fetchCurrentUser = useCallback(
    async (token: string) => {
      try {
        setLoadingUser(true);

        const user = await getCurrentUser(token);
        setCurrentUser(user);

        await loadConversations(token);
        setMessage("");
      } catch (error) {
        localStorage.removeItem("token");
        clearUserSubscriptions();
        setCurrentUser(null);
        setConversations([]);
        setSelectedConversation(null);
        setMessage(
          error instanceof Error ? error.message : "Could not load user data",
        );
      } finally {
        setLoadingUser(false);
      }
    },
    [
      loadConversations,
      clearUserSubscriptions,
      setConversations,
      setSelectedConversation,
    ],
  );

  const handleLogin = async (payload: { login: string; password: string }) => {
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
      setMessage(
        error instanceof Error ? error.message : "Registration failed",
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProfile = async (payload: {
    displayName: string;
    avatarUrl: string | null;
  }) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");

      const updatedUser = await updateProfile(token, payload);
      setCurrentUser(updatedUser);

      await loadConversations(token, selectedConversation?.id || undefined);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not update profile",
      );
    }
  };

  const handleChangePassword = async (payload: {
    currentPassword: string;
    newPassword: string;
  }) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");
      await changePassword(token, payload);
      setMessage("Password changed successfully");
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not change password",
      );
    }
  };

  // Clears local auth data and resets all user-specific UI state.
  const handleLogout = () => {
    clearUserSubscriptions();
    localStorage.removeItem("token");

    setCurrentUser(null);
    setSearchResults([]);
    setConversations([]);
    setSelectedConversation(null);
    setIsCreateGroupOpen(false);
    setIsManageGroupOpen(false);
    setIsProfileOpen(false);
    setMessage("Logged out");
    setMode("login");
  };

  // Restores the session after page refresh if a token is still available.
  useEffect(() => {
    const savedToken = localStorage.getItem("token");

    if (!savedToken) {
      setLoadingUser(false);
      return;
    }

    void fetchCurrentUser(savedToken);
  }, [fetchCurrentUser]);

  // Periodically refreshes conversations as a fallback in case a WebSocket update is missed.
  useEffect(() => {
    if (!currentUser) return;

    const interval = setInterval(() => {
      void refreshConversations();
    }, 5000);

    return () => clearInterval(interval);
  }, [currentUser?.id, refreshConversations]);

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
              isProfileOpen={isProfileOpen}
              onOpenProfile={() => setIsProfileOpen(true)}
              onCloseProfile={() => setIsProfileOpen(false)}
              onSaveProfile={handleSaveProfile}
              onChangePassword={handleChangePassword}
              searchResults={searchResults}
              searchLoading={searchLoading}
              conversations={conversations}
              selectedConversation={selectedConversation}
              isCreateGroupOpen={isCreateGroupOpen}
              onOpenCreateGroup={() => setIsCreateGroupOpen(true)}
              onCloseCreateGroup={handleCloseCreateGroup}
              onSearchUsers={handleSearchUsers}
              onStartConversation={handleStartConversation}
              onCreateGroup={handleCreateGroup}
              onSelectConversation={setSelectedConversation}
              onRefreshConversations={refreshConversations}
              isManageGroupOpen={isManageGroupOpen}
              onOpenManageGroup={() => setIsManageGroupOpen(true)}
              onCloseManageGroup={() => setIsManageGroupOpen(false)}
              onUpdateGroupName={handleUpdateGroupName}
              onAddParticipant={handleAddParticipant}
              onRemoveParticipant={handleRemoveParticipant}
              onLeaveGroup={handleLeaveGroup}
              onTransferOwnership={handleTransferOwnership}
              onDeleteGroup={handleDeleteGroup}
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
