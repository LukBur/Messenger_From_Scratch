"use client";

import { useEffect, useState } from "react";
import { UserResponse } from "@/types/user";

type ProfileModalProps = {
  isOpen: boolean;
  currentUser: UserResponse | null;
  onClose: () => void;
  onSave: (payload: {
    displayName: string;
    avatarUrl: string | null;
  }) => Promise<void>;
  onChangePassword: (payload: {
    currentPassword: string;
    newPassword: string;
  }) => Promise<void>;
};

export default function ProfileModal({
  isOpen,
  currentUser,
  onClose,
  onSave,
  onChangePassword,
}: ProfileModalProps) {
  const [displayName, setDisplayName] = useState("");
  const [avatarUrl, setAvatarUrl] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");

  // Resets form fields whenever the modal is opened for the current user.
  useEffect(() => {
    if (!currentUser) return;

    setDisplayName(currentUser.displayName);
    setAvatarUrl(currentUser.avatarUrl || "");
    setCurrentPassword("");
    setNewPassword("");
  }, [currentUser, isOpen]);

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    await onSave({
      displayName: displayName.trim(),
      
      // Empty avatar input is saved as null so the backend can treat it as no avatar.
      avatarUrl: avatarUrl.trim() ? avatarUrl.trim() : null,
    });

    onClose();
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    await onChangePassword({
      currentPassword,
      newPassword,
    });

    setCurrentPassword("");
    setNewPassword("");
  };

  if (!isOpen || !currentUser) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <p className="eyebrow">Account settings</p>
            <h2>Profile</h2>
          </div>

          <button className="secondary-button" type="button" onClick={onClose}>
            Close
          </button>
        </div>

        <form className="group-form" onSubmit={handleProfileSubmit}>
          <div className="form-group">
            <label>Email</label>
            <input type="text" value={currentUser.email} disabled />
          </div>

          <div className="form-group">
            <label>Login</label>
            <input type="text" value={currentUser.login} disabled />
          </div>

          <div className="form-group">
            <label>Display name</label>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label>Avatar URL</label>
            <input
              type="text"
              value={avatarUrl}
              onChange={(e) => setAvatarUrl(e.target.value)}
              placeholder="https://..."
            />
          </div>

          {avatarUrl.trim() && (
            <div className="profile-avatar-preview">
              <img src={avatarUrl} alt="Avatar preview" />
            </div>
          )}

          <button className="primary-button" type="submit">
            Save profile
          </button>
        </form>

        <section className="manage-group-section">
          <h3 className="section-title">Change password</h3>

          <form className="group-form" onSubmit={handlePasswordSubmit}>
            <div className="form-group">
              <label>Current password</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label>New password</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </div>

            <button className="primary-button" type="submit">
              Change password
            </button>
          </form>
        </section>
      </div>
    </div>
  );
}
