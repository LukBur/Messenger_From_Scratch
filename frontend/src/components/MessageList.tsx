import { MessageResponse } from "@/types/message";
import { UserResponse } from "@/types/user";

type MessageListProps = {
  messages: MessageResponse[];
  currentUser: UserResponse;
};

function formatDate(dateString: string) {
  const date = new Date(dateString);
  return date.toLocaleString();
}

export default function MessageList({
  messages,
  currentUser,
}: MessageListProps) {
  if (messages.length === 0) {
    return <p className="muted-text">No messages yet.</p>;
  }

  return (
    <div className="message-list">
      {messages.map((message) => {
        const isOwn = message.sender.id === currentUser.id;

        return (
          <div
            key={message.id}
            className={isOwn ? "message-bubble own" : "message-bubble"}
          >
            <div className="message-meta">
              <strong>{message.sender.displayName}</strong>
              <span>{formatDate(message.createdAt)}</span>
            </div>

            <p className="message-content">{message.content}</p>

            {message.edited && (
              <span className="message-edited-label">edited</span>
            )}
          </div>
        );
      })}
    </div>
  );
}