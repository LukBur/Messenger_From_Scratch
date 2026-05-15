"use client";

import { useState } from "react";
import { UserSearchResponse } from "@/types/user";

type CreateGroupModalProps = {
  isOpen: boolean;
  onClose: () => void;
  users: UserSearchResponse[];
  loading: boolean;
  onSearch: (query: string) => Promise<void>;
  onCreateGroup: (payload: {
    name: string;
    participantIds: string[];
  }) => Promise<void>;
};

export default function CreateGroupModal({
  isOpen,
  onClose,
  users,
  loading,
  onSearch,
  onCreateGroup,
}: CreateGroupModalProps) {
  const [query, setQuery] = useState("");
  const [groupName, setGroupName] = useState("");
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);

  const handleToggleUser = (userId: string) => {
    setSelectedUserIds((prev) =>
      prev.includes(userId)
        ? prev.filter((id) => id !== userId)
        : [...prev, userId]
    );
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSearch(query);
  };

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!groupName.trim()) return;
    if (selectedUserIds.length < 2) return;

    await onCreateGroup({
      name: groupName.trim(),
      participantIds: selectedUserIds,
    });

    setGroupName("");
    setQuery("");
    setSelectedUserIds([]);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-card"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <p className="eyebrow">New conversation</p>
            <h2>Create group</h2>
          </div>

          <button className="secondary-button" onClick={onClose} type="button">
            Close
          </button>
        </div>

        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="text"
            placeholder="Search users for group"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button className="secondary-button" type="submit" disabled={loading}>
            {loading ? "Searching..." : "Search users"}
          </button>
        </form>

        <form className="group-form" onSubmit={handleCreateGroup}>
          <input
            type="text"
            placeholder="Group name"
            value={groupName}
            onChange={(e) => setGroupName(e.target.value)}
          />

          <div className="group-users-list">
            {users.length === 0 ? (
              <p className="muted-text">Search users to add them to a group.</p>
            ) : (
              users.map((user) => {
                const selected = selectedUserIds.includes(user.id);

                return (
                  <label key={user.id} className="group-user-item">
                    <input
                      type="checkbox"
                      checked={selected}
                      onChange={() => handleToggleUser(user.id)}
                    />
                    <span>
                      {user.displayName}{" "}
                      <span className="muted-text">@{user.login}</span>
                    </span>
                  </label>
                );
              })
            )}
          </div>

          <button
            className="primary-button"
            type="submit"
            disabled={!groupName.trim() || selectedUserIds.length < 2}
          >
            Create group
          </button>
        </form>
      </div>
    </div>
  );
}