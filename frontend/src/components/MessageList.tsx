"use client";

import { useEffect, useState } from "react";
import { MessageResponse } from "@/types/message";
import { UserResponse } from "@/types/user";
import { formatDate, getRemainingSeconds } from "@/utils/messageHelpers";

type MessageListProps = {
  messages: MessageResponse[];
  currentUser: UserResponse;
  onEditMessage: (messageId: string, content: string) => Promise<void>;
  onDeleteMessage: (messageId: string) => Promise<void>;
};

export default function MessageList({
  messages,
  currentUser,
  onEditMessage,
  onDeleteMessage,
}: MessageListProps) {
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editedContent, setEditedContent] = useState("");

  // Used only to trigger rerenders for disappearing message countdown updates.
  const [tick, setTick] = useState(0);

  // Starts a timer only when at least one disappearing message is visible.
  useEffect(() => {
    const hasDisappearingMessages = messages.some((msg) => msg.disappearing);

    if (!hasDisappearingMessages) return;

    const interval = setInterval(() => {
      setTick((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [messages]);

  // Pre-fills the edit form with the current message content.
  const startEditing = (message: MessageResponse) => {
    setEditingMessageId(message.id);
    setEditedContent(message.content);
  };

  // Clears edit mode state after saving or cancelling message edits.
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

    // Prevents keeping edit mode open for messages that were deleted.
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

        // Accessing tick forces the component to rerender every second for countdown updates.
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
