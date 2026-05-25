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
  const [selectedUsers, setSelectedUsers] = useState<UserSearchResponse[]>([]);

  const isSelected = (userId: string) =>
    selectedUsers.some((user) => user.id === userId);

  const handleToggleUser = (user: UserSearchResponse) => {
    setSelectedUsers((prev) =>
      prev.some((item) => item.id === user.id)
        ? prev.filter((item) => item.id !== user.id)
        : [...prev, user],
    );
  };

  const handleRemoveSelectedUser = (userId: string) => {
    setSelectedUsers((prev) => prev.filter((user) => user.id !== userId));
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSearch(query);
  };

  const handleCreateGroup = async () => {
    if (!groupName.trim()) return;
    if (selectedUsers.length < 2) return;

    await onCreateGroup({
      name: groupName.trim(),
      participantIds: selectedUsers.map((user) => user.id),
    });

    setGroupName("");
    setQuery("");
    setSelectedUsers([]);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <p className="eyebrow">New conversation</p>
            <h2>Create group</h2>
          </div>

          <button className="secondary-button" onClick={onClose} type="button">
            Close
          </button>
        </div>

        <div className="group-form">
          <input
            type="text"
            placeholder="Group name"
            value={groupName}
            onChange={(e) => setGroupName(e.target.value)}
          />

          <div className="selected-users-section">
            <h3 className="section-title">Selected users</h3>

            {selectedUsers.length === 0 ? (
              <p className="muted-text">No users selected yet.</p>
            ) : (
              <div className="selected-users-list">
                {selectedUsers.map((user) => (
                  <div key={user.id} className="selected-user-chip">
                    <span>
                      {user.displayName}{" "}
                      <span className="muted-text">@{user.login}</span>
                    </span>
                    <button
                      type="button"
                      className="chip-remove-button"
                      onClick={() => handleRemoveSelectedUser(user.id)}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <form className="search-form" onSubmit={handleSearch}>
            <input
              type="text"
              placeholder="Search users for group"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <button
              className="secondary-button"
              type="submit"
              disabled={loading}
            >
              {loading ? "Searching..." : "Search users"}
            </button>
          </form>

          <div className="group-users-list">
            {users.length === 0 ? (
              <p className="muted-text">Search users to add them to a group.</p>
            ) : (
              users.map((user) => {
                const selected = isSelected(user.id);

                return (
                  <label key={user.id} className="group-user-item">
                    <input
                      type="checkbox"
                      checked={selected}
                      onChange={() => handleToggleUser(user)}
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
            type="button"
            onClick={handleCreateGroup}
            disabled={!groupName.trim() || selectedUsers.length < 2}
          >
            Create group
          </button>
        </div>
      </div>
    </div>
  );
}
