"use client";

import { FormEvent, useState } from "react";

type MessageComposerProps = {
  onSendMessage: (
    content: string,
    disappearAfterSeconds?: number,
  ) => Promise<void>;
  loading: boolean;
};

export default function MessageComposer({
  onSendMessage,
  loading,
}: MessageComposerProps) {
  const [content, setContent] = useState("");
  const [disappearAfterSeconds, setDisappearAfterSeconds] = useState(0);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const trimmed = content.trim();
    if (!trimmed) return;

    await onSendMessage(
      trimmed,
      disappearAfterSeconds > 0 ? disappearAfterSeconds : undefined,
    );

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
      <select
        className="message-type-select"
        value={disappearAfterSeconds}
        onChange={(e) => setDisappearAfterSeconds(Number(e.target.value))}
      >
        <option value={0}>Normal message</option>
        <option value={10}>Disappear after 10s</option>
        <option value={30}>Disappear after 30s</option>
        <option value={60}>Disappear after 1m</option>
        <option value={300}>Disappear after 5m</option>
      </select>
      <button className="primary-button" type="submit" disabled={loading}>
        {loading ? "Sending..." : "Send"}
      </button>
    </form>
  );
}