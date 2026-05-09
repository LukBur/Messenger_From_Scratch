"use client";

import { useState } from "react";
import { UserSearchResponse } from "@/types/user";

type UserSearchProps = {
  results: UserSearchResponse[];
  loading: boolean;
  onSearch: (query: string) => Promise<void>;
  onStartConversation: (userId: string) => Promise<void>;
};

export default function UserSearch({
  results,
  loading,
  onSearch,
  onStartConversation,
}: UserSearchProps) {
  const [query, setQuery] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSearch(query);
  };

  return (
    <section className="sidebar-card">
      <h2 className="section-title">Find users</h2>

      <form className="search-form" onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Search by login or display name"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? "Searching..." : "Search"}
        </button>
      </form>

      <div className="search-results">
        {results.length === 0 ? (
          <p className="muted-text">No results yet.</p>
        ) : (
          results.map((user) => (
            <div className="search-user-card" key={user.id}>
              <div>
                <strong>{user.displayName}</strong>
                <p className="muted-text">@{user.login}</p>
              </div>

              <button
                className="secondary-button"
                onClick={() => onStartConversation(user.id)}
              >
                Chat
              </button>
            </div>
          ))
        )}
      </div>
    </section>
  );
}