"use client";

import { ConversationResponse } from "@/types/conversation";
import { UserSearchResponse, UserResponse } from "@/types/user";
import UserSearch from "@/components/UserSearch";
import ConversationList from "@/components/ConversationList";
import ConversationView from "@/components/ConversationView";
import CreateGroupModal from "@/components/CreateGroupModal";
import ManageGroupModal from "@/components/ManageGroupModal";
import ProfileModal from "@/components/ProfileModal";

type ChatLayoutProps = {
  currentUser: UserResponse;

  isProfileOpen: boolean;
  onOpenProfile: () => void;
  onCloseProfile: () => void;
  onSaveProfile: (payload: {
    displayName: string;
    avatarUrl: string | null;
  }) => Promise<void>;

  searchResults: UserSearchResponse[];
  searchLoading: boolean;
  conversations: ConversationResponse[];
  selectedConversation: ConversationResponse | null;
  isCreateGroupOpen: boolean;
  onOpenCreateGroup: () => void;
  onCloseCreateGroup: () => void;
  onSearchUsers: (query: string) => Promise<void>;
  onStartConversation: (userId: string) => Promise<void>;
  onCreateGroup: (payload: {
    name: string;
    participantIds: string[];
  }) => Promise<void>;
  onSelectConversation: (conversation: ConversationResponse) => void;
  onRefreshConversations: () => Promise<void>;

  isManageGroupOpen: boolean;
  onOpenManageGroup: () => void;
  onCloseManageGroup: () => void;
  onUpdateGroupName: (conversationId: string, name: string) => Promise<void>;
  onAddParticipant: (conversationId: string, userId: string) => Promise<void>;
  onRemoveParticipant: (
    conversationId: string,
    userId: string,
  ) => Promise<void>;

  onChangePassword: (payload: {
    currentPassword: string;
    newPassword: string;
  }) => Promise<void>;
  onLogout: () => void;
};

export default function ChatLayout({
  currentUser,

  isProfileOpen,
  onOpenProfile,
  onCloseProfile,
  onSaveProfile,

  searchResults,
  searchLoading,
  conversations,
  selectedConversation,
  isCreateGroupOpen,
  onOpenCreateGroup,
  onCloseCreateGroup,
  onSearchUsers,
  onStartConversation,
  onCreateGroup,
  onSelectConversation,
  onRefreshConversations,

  isManageGroupOpen,
  onOpenManageGroup,
  onCloseManageGroup,
  onUpdateGroupName,
  onAddParticipant,
  onRemoveParticipant,
  onChangePassword,
  onLogout,
}: ChatLayoutProps) {
  return (
    <>
      <section className="chat-shell">
        <header className="chat-topbar">
          <div className="topbar-user">
            <div className="topbar-avatar">
              {currentUser.avatarUrl ? (
                <img
                  src={currentUser.avatarUrl}
                  alt={currentUser.displayName}
                />
              ) : (
                <span>{currentUser.displayName.charAt(0).toUpperCase()}</span>
              )}
            </div>

            <div>
              <p className="eyebrow">Best Communicator</p>
              <h1 className="chat-title">Welcome, {currentUser.displayName}</h1>
            </div>
          </div>

          <div className="topbar-actions">
            <button className="secondary-button" onClick={onOpenProfile}>
              Profile
            </button>
            <button className="primary-button" onClick={onOpenCreateGroup}>
              Create group
            </button>
            <button className="secondary-button" onClick={onLogout}>
              Log out
            </button>
          </div>
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
              onOpenManageGroup={onOpenManageGroup}
            />
          </div>
        </div>
      </section>

      <CreateGroupModal
        isOpen={isCreateGroupOpen}
        onClose={onCloseCreateGroup}
        users={searchResults}
        loading={searchLoading}
        onSearch={onSearchUsers}
        onCreateGroup={onCreateGroup}
      />
      <ProfileModal
        isOpen={isProfileOpen}
        currentUser={currentUser}
        onClose={onCloseProfile}
        onSave={onSaveProfile}
        onChangePassword={onChangePassword}
      />
      <ManageGroupModal
        isOpen={isManageGroupOpen}
        conversation={selectedConversation}
        currentUser={currentUser}
        searchResults={searchResults}
        loading={searchLoading}
        onClose={onCloseManageGroup}
        onSearchUsers={onSearchUsers}
        onUpdateGroupName={onUpdateGroupName}
        onAddParticipant={onAddParticipant}
        onRemoveParticipant={onRemoveParticipant}
      />
    </>
  );
}