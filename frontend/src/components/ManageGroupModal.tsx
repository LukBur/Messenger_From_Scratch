"use client";

import { useMemo, useState } from "react";
import { ConversationResponse } from "@/types/conversation";
import { UserResponse, UserSearchResponse } from "@/types/user";

type ManageGroupModalProps = {
  isOpen: boolean;
  conversation: ConversationResponse | null;
  currentUser: UserResponse;
  searchResults: UserSearchResponse[];
  loading: boolean;
  onClose: () => void;
  onSearchUsers: (query: string) => Promise<void>;
  onUpdateGroupName: (conversationId: string, name: string) => Promise<void>;
  onAddParticipant: (conversationId: string, userId: string) => Promise<void>;
  onRemoveParticipant: (
    conversationId: string,
    userId: string,
  ) => Promise<void>;
};

export default function ManageGroupModal({
  isOpen,
  conversation,
  currentUser,
  searchResults,
  loading,
  onClose,
  onSearchUsers,
  onUpdateGroupName,
  onAddParticipant,
  onRemoveParticipant,
}: ManageGroupModalProps) {
  const [query, setQuery] = useState("");
  const [groupName, setGroupName] = useState(conversation?.name || "");

  const isGroup = conversation?.type === "GROUP";
  const isOwner = conversation?.ownerId === currentUser.id;

  const participantIds = useMemo(
    () => new Set(conversation?.participants.map((p) => p.id) || []),
    [conversation],
  );

  const searchableUsers = useMemo(() => {
    return searchResults.filter((user) => !participantIds.has(user.id));
  }, [searchResults, participantIds]);

  if (!isOpen || !conversation || !isGroup) return null;

  const handleUpdateName = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!groupName.trim()) return;

    await onUpdateGroupName(conversation.id, groupName.trim());
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSearchUsers(query);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <p className="eyebrow">Group settings</p>
            <h2>{conversation.name || "Group conversation"}</h2>
          </div>

          <button className="secondary-button" type="button" onClick={onClose}>
            Close
          </button>
        </div>

        <section className="manage-group-section">
          <h3 className="section-title">Participants</h3>

          <div className="group-users-list">
            {conversation.participants.map((participant) => {
              const isParticipantOwner =
                participant.id === conversation.ownerId;
              const canRemove =
                isOwner &&
                participant.id !== conversation.ownerId &&
                conversation.participants.length > 3;

              return (
                <div key={participant.id} className="group-user-item">
                  <div>
                    <strong>{participant.displayName}</strong>{" "}
                    <span className="muted-text">@{participant.login}</span>
                    {isParticipantOwner && (
                      <span className="group-owner-badge">Owner</span>
                    )}
                  </div>

                  {canRemove && (
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() =>
                        onRemoveParticipant(conversation.id, participant.id)
                      }
                    >
                      Remove
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </section>

        {isOwner && (
          <>
            <section className="manage-group-section">
              <h3 className="section-title">Rename group</h3>

              <form className="group-form" onSubmit={handleUpdateName}>
                <input
                  type="text"
                  value={groupName}
                  onChange={(e) => setGroupName(e.target.value)}
                  placeholder="Group name"
                />
                <button className="primary-button" type="submit">
                  Save name
                </button>
              </form>
            </section>

            <section className="manage-group-section">
              <h3 className="section-title">Add participants</h3>

              <form className="search-form" onSubmit={handleSearch}>
                <input
                  type="text"
                  placeholder="Search users"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
                <button
                  className="secondary-button"
                  type="submit"
                  disabled={loading}
                >
                  {loading ? "Searching..." : "Search"}
                </button>
              </form>

              <div className="group-users-list">
                {searchableUsers.length === 0 ? (
                  <p className="muted-text">No users to add yet.</p>
                ) : (
                  searchableUsers.map((user) => (
                    <div key={user.id} className="group-user-item">
                      <div>
                        <strong>{user.displayName}</strong>{" "}
                        <span className="muted-text">@{user.login}</span>
                      </div>

                      <button
                        type="button"
                        className="primary-button"
                        onClick={() =>
                          onAddParticipant(conversation.id, user.id)
                        }
                      >
                        Add
                      </button>
                    </div>
                  ))
                )}
              </div>
            </section>
          </>
        )}

        {!isOwner && (
          <p className="muted-text">
            Only the group owner can manage this group.
          </p>
        )}
      </div>
    </div>
  );
}
