"use client";

import { FormEvent, useState } from "react";

type MessageComposerProps = {
  onSendMessage: (content: string) => Promise<void>;
  loading: boolean;
};

export default function MessageComposer({
  onSendMessage,
  loading,
}: MessageComposerProps) {
  const [content, setContent] = useState("");

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const trimmed = content.trim();
    if (!trimmed) return;

    await onSendMessage(trimmed);
    setContent("");
  };

  return (
    <form className="message-composer" onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="Type your message..."
        value={content}
        onChange={(e) => setContent(e.target.value)}
      />

      <button className="primary-button" type="submit" disabled={loading}>
        {loading ? "Sending..." : "Send"}
      </button>
    </form>
  );
}