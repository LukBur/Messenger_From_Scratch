import { UserResponse } from "@/types/user";

type UserPanelProps = {
  user: UserResponse;
  onLogout: () => void;
};

export default function UserPanel({ user, onLogout }: UserPanelProps) {
  return (
    <section className="panel-card">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Logged in</p>
          <h1>{user.displayName}</h1>
        </div>

        <button className="secondary-button" onClick={onLogout}>
          Log out
        </button>
      </div>

      <div className="user-grid">
        <div className="info-card">
          <span className="info-label">Login</span>
          <span>{user.login}</span>
        </div>

        <div className="info-card">
          <span className="info-label">Email</span>
          <span>{user.email}</span>
        </div>

        <div className="info-card">
          <span className="info-label">Role</span>
          <span>{user.role}</span>
        </div>

        <div className="info-card">
          <span className="info-label">Avatar</span>
          <span>{user.avatarUrl ? user.avatarUrl : "Not set"}</span>
        </div>
      </div>
    </section>
  );
}