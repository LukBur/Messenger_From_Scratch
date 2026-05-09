"use client";

import { ConversationResponse } from "@/types/conversation";
import { UserSearchResponse, UserResponse } from "@/types/user";
import UserSearch from "@/components/UserSearch";
import ConversationList from "@/components/ConversationList";
import ConversationView from "@/components/ConversationView";

type ChatLayoutProps = {
  currentUser: UserResponse;
  searchResults: UserSearchResponse[];
  searchLoading: boolean;
  conversations: ConversationResponse[];
  selectedConversation: ConversationResponse | null;
  onSearchUsers: (query: string) => Promise<void>;
  onStartConversation: (userId: string) => Promise<void>;
  onSelectConversation: (conversation: ConversationResponse) => void;
  onRefreshConversations: () => Promise<void>;
  onLogout: () => void;
};

export default function ChatLayout({
  currentUser,
  searchResults,
  searchLoading,
  conversations,
  selectedConversation,
  onSearchUsers,
  onStartConversation,
  onSelectConversation,
  onRefreshConversations,
  onLogout,
}: ChatLayoutProps) {
  return (
    <section className="chat-shell">
      <header className="chat-topbar">
        <div>
          <p className="eyebrow">Best Communicator</p>
          <h1 className="chat-title">Welcome, {currentUser.displayName}</h1>
        </div>

        <button className="secondary-button" onClick={onLogout}>
          Log out
        </button>
      </header>

      <div className="chat-grid">
        <aside className="chat-sidebar">
          <UserSearch
            results={searchResults}
            loading={searchLoading}
            onSearch={onSearchUsers}
            onStartConversation={onStartConversation}
          />

          <ConversationList
            conversations={conversations}
            currentUser={currentUser}
            selectedConversationId={selectedConversation?.id || null}
            onSelectConversation={onSelectConversation}
          />
        </aside>

        <div className="chat-content">
          <ConversationView
            conversation={selectedConversation}
            currentUser={currentUser}
            onConversationUpdated={onRefreshConversations}
          />
        </div>
      </div>
    </section>
  );
}