"use client";

import { useState } from "react";
import { MessageResponse } from "@/types/message";
import { UserResponse } from "@/types/user";

type MessageListProps = {
  messages: MessageResponse[];
  currentUser: UserResponse;
  onEditMessage: (messageId: string, content: string) => Promise<void>;
};

function formatDate(dateString: string) {
  const date = new Date(dateString);
  return date.toLocaleString();
}

export default function MessageList({
  messages,
  currentUser,
  onEditMessage,
}: MessageListProps) {
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editedContent, setEditedContent] = useState("");

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

  if (messages.length === 0) {
    return <p className="muted-text">No messages yet.</p>;
  }

  return (
    <div className="message-list">
      {messages.map((message) => {
        const isOwn = message.sender.id === currentUser.id;
        const isEditing = editingMessageId === message.id;

        return (
          <div
            key={message.id}
            className={isOwn ? "message-bubble own" : "message-bubble"}
          >
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

                <div className="message-footer">
                  {message.edited && (
                    <span className="message-edited-label">edited</span>
                  )}

                  {isOwn && (
                    <button
                      type="button"
                      className="message-edit-button"
                      onClick={() => startEditing(message)}
                    >
                      Edit
                    </button>
                  )}
                </div>
              </>
            )}
          </div>
        );
      })}
    </div>
  );
}