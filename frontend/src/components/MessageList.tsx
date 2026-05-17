"use client";

import { useEffect, useState } from "react";
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
function parseBackendDate(dateString: string): Date {
  // Parsuje string bezpiecznie. Jeśli zawiera zbyt wiele cyfr po kropce,
  // Date.parse poradzi sobie z tym stabilniej.
  const timestamp = Date.parse(dateString);
  return isNaN(timestamp) ? new Date() : new Date(timestamp);
}

function isExpired(message: MessageResponse) {
  if (!message.disappearing || !message.expiresAt) {
    return false;
  }
  // Używamy bezpiecznego parsowania
  return parseBackendDate(message.expiresAt).getTime() < Date.now();
}

function getRemainingSeconds(expiresAt: string | null) {
  if (!expiresAt) return 0;

  // Używamy bezpiecznego parsowania
  const diff = parseBackendDate(expiresAt).getTime() - Date.now();

  return Math.max(Math.floor(diff / 1000), 0);
}

export default function MessageList({
                                      messages,
                                      currentUser,
                                      onEditMessage,
                                    }: MessageListProps) {
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editedContent, setEditedContent] = useState("");

  // Poprawiony licznik do wymuszania re-renderu komponentu
  const [tick, setTick] = useState(0);

  // 🔴 KLUCZOWA POPRAWKA: Ten efekt co 1 sekundę odświeża komponent,
  // dzięki czemu odliczanie działa na żywo, a wygasłe wiadomości znikają same!
  useEffect(() => {
    // Sprawdzamy czy w ogóle jest jakakolwiek znikająca wiadomość na liście
    const hasDisappearingMessages = messages.some(msg => msg.disappearing);

    if (!hasDisappearingMessages) return;

    const interval = setInterval(() => {
      setTick((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(interval); // Czyszczenie interwału przy unmouncie
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

  // Filtrujemy wiadomości na bieżąco (teraz odpala się to co sekundę dzięki `tick`)
  const activeMessages = messages.filter((message) => !isExpired(message));

  if (activeMessages.length === 0) {
    return <p className="muted-text">No messages yet.</p>;
  }

  return (
      <div className="message-list">
        {activeMessages.map((message) => {
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

                      {message.disappearing && (
                          <span className="message-expire-timer">
                    disappears in {getRemainingSeconds(message.expiresAt)}s
                  </span>
                      )}

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