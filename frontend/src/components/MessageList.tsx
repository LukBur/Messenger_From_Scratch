"use client";

import { useEffect, useState } from "react";
import { MessageResponse } from "@/types/message";
import { UserResponse } from "@/types/user";

type MessageListProps = {
  messages: MessageResponse[];
  currentUser: UserResponse;
  onEditMessage: (messageId: string, content: string) => Promise<void>;
  onDeleteMessage: (messageId: string) => Promise<void>;
};

function formatDate(dateString: string) {
  const date = new Date(dateString);
  return date.toLocaleString();
}

function parseBackendDate(dateString: string): Date {
  const timestamp = Date.parse(dateString);
  return isNaN(timestamp) ? new Date() : new Date(timestamp);
}

function getRemainingSeconds(expiresAt: string | null) {
  if (!expiresAt) return 0;

  const diff = parseBackendDate(expiresAt).getTime() - Date.now();
  return Math.max(Math.floor(diff / 1000), 0);
}

export default function MessageList({
  messages,
  currentUser,
  onEditMessage,
  onDeleteMessage,
}: MessageListProps) {
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editedContent, setEditedContent] = useState("");
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const hasDisappearingMessages = messages.some((msg) => msg.disappearing);

    if (!hasDisappearingMessages) return;

    const interval = setInterval(() => {
      setTick((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [messages]);

  const startEditing = (message: MessageResponse) => {
    setEditingMessageId(message.id);
    setEditedContent(message.content);
  };

  const cancelEditing = () => {
    setEditingMessageId(null);
    setEditedContent("");
  };

  const saveEditing = async () => {
    if (!editingMessageId) return;

    const trimmed = editedContent.trim();
    if (!trimmed) return;

    await onEditMessage(editingMessageId, trimmed);
    cancelEditing();
  };

  const handleDelete = async (messageId: string) => {
    await onDeleteMessage(messageId);

    if (editingMessageId === messageId) {
      cancelEditing();
    }
  };

  if (messages.length === 0) {
    return <p className="muted-text">No messages yet.</p>;
  }

  return (
    <div className="message-list">
      {messages.map((message) => {
        const isOwn = message.sender.id === currentUser.id;
        const isEditing = editingMessageId === message.id;
        void tick;

        return (
          <div
            key={message.id}
            className={isOwn ? "message-row own" : "message-row"}
          >
            {!isOwn && (
              <div className="message-avatar">
                {message.sender.avatarUrl ? (
                  <img
                    src={message.sender.avatarUrl}
                    alt={message.sender.displayName}
                  />
                ) : (
                  <span>
                    {message.sender.displayName.charAt(0).toUpperCase()}
                  </span>
                )}
              </div>
            )}

            <div className={isOwn ? "message-bubble own" : "message-bubble"}>
              <div className="message-meta">
                <strong>{message.sender.displayName}</strong>
                <span>{formatDate(message.createdAt)}</span>
              </div>

              {isEditing ? (
                <div className="message-edit-box">
                  <textarea
                    value={editedContent}
                    onChange={(e) => setEditedContent(e.target.value)}
                    rows={3}
                  />
                  <div className="message-edit-actions">
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={cancelEditing}
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      className="primary-button"
                      onClick={saveEditing}
                    >
                      Save
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <p className="message-content">{message.content}</p>

                  {message.disappearing && (
                    <span className="message-expire-timer">
                      disappears in {getRemainingSeconds(message.expiresAt)}s
                    </span>
                  )}

                  <div className="message-footer">
                    <div>
                      {message.edited && (
                        <span className="message-edited-label">edited</span>
                      )}
                    </div>

                    {isOwn && (
                      <div className="message-actions">
                        <button
                          type="button"
                          className="message-edit-button"
                          onClick={() => startEditing(message)}
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          className="message-delete-button"
                          onClick={() => handleDelete(message.id)}
                        >
                          Delete
                        </button>
                      </div>
                    )}
                  </div>
                </>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
