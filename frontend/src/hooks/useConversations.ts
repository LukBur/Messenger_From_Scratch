"use client";

import { useCallback, useRef, useState } from "react";
import {
  addParticipantToGroup,
  createGroupConversation,
  createPrivateConversation,
  deleteGroup,
  getMyConversations,
  leaveGroup,
  removeParticipantFromGroup,
  searchUsers,
  transferGroupOwnership,
  updateGroupName,
} from "@/lib/api";
import { ConversationResponse } from "@/types/conversation";
import { UserSearchResponse } from "@/types/user";

type UseConversationsParams = {
  setMessage: (message: string) => void;
  onCloseManageGroup: () => void;
  onCloseCreateGroup: () => void;
};

export function useConversations({
  setMessage,
  onCloseManageGroup,
  onCloseCreateGroup,
}: UseConversationsParams) {
  const [searchResults, setSearchResults] = useState<UserSearchResponse[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [conversations, setConversations] = useState<ConversationResponse[]>(
    [],
  );
  const [selectedConversation, setSelectedConversation] =
    useState<ConversationResponse | null>(null);

  const selectedConversationIdRef = useRef<string | null>(null);

  const setSelectedConversationWithRef = useCallback(
    (
      value:
        | ConversationResponse
        | null
        | ((prev: ConversationResponse | null) => ConversationResponse | null),
    ) => {
      setSelectedConversation((prev) => {
        const nextValue = typeof value === "function" ? value(prev) : value;

        selectedConversationIdRef.current = nextValue?.id ?? null;
        return nextValue;
      });
    },
    [],
  );

  const loadConversations = useCallback(
    async (token: string, preferredConversationId?: string) => {
      const data = await getMyConversations(token);
      setConversations(data);

      if (data.length === 0) {
        setSelectedConversationWithRef(null);
        return;
      }

      const targetId =
        preferredConversationId ?? selectedConversationIdRef.current;

      const matchedConversation = data.find((item) => item.id === targetId);

      if (matchedConversation) {
        setSelectedConversationWithRef(matchedConversation);
      } else {
        setSelectedConversationWithRef(data[0]);
      }
    },
    [],
  );

  const refreshConversations = useCallback(async () => {
    const token = localStorage.getItem("token");
    if (!token) return;

    await loadConversations(token);
  }, [loadConversations]);

  const handleSearchUsers = async (query: string) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setSearchLoading(true);
      setMessage("");

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
        error instanceof Error
          ? error.message
          : "Could not create conversation",
      );
    }
  };

  const handleCreateGroup = async (payload: {
    name: string;
    participantIds: string[];
  }) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");

      const conversation = await createGroupConversation(token, payload);
      await loadConversations(token, conversation.id);
      setSearchResults([]);
      onCloseCreateGroup();
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not create group",
      );
    }
  };

  const handleUpdateGroupName = async (
    conversationId: string,
    name: string,
  ) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");
      await updateGroupName(token, conversationId, name);
      await loadConversations(token, conversationId);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not update group name",
      );
    }
  };

  const handleAddParticipant = async (
    conversationId: string,
    userId: string,
  ) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");
      await addParticipantToGroup(token, conversationId, userId);
      await loadConversations(token, conversationId);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not add participant",
      );
    }
  };

  const handleRemoveParticipant = async (
    conversationId: string,
    userId: string,
  ) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");
      await removeParticipantFromGroup(token, conversationId, userId);
      await loadConversations(token, conversationId);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not remove participant",
      );
    }
  };

  const handleLeaveGroup = async (conversationId: string) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");
      await leaveGroup(token, conversationId);

      if (selectedConversationIdRef.current === conversationId) {
        setSelectedConversationWithRef(null);
        onCloseManageGroup();
      }

      await loadConversations(token);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not leave group",
      );
    }
  };

  const handleTransferOwnership = async (
    conversationId: string,
    newOwnerId: string,
  ) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");
      await transferGroupOwnership(token, conversationId, newOwnerId);
      await loadConversations(token, conversationId);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not transfer ownership",
      );
    }
  };

  const handleDeleteGroup = async (conversationId: string) => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setMessage("");
      await deleteGroup(token, conversationId);

      if (selectedConversationIdRef.current === conversationId) {
        setSelectedConversationWithRef(null);
        onCloseManageGroup();
      }

      await loadConversations(token);
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : "Could not delete group",
      );
    }
  };

  return {
    searchResults,
    searchLoading,
    conversations,
    selectedConversation,
    setConversations,
    setSelectedConversation: setSelectedConversationWithRef,
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
  };
}
