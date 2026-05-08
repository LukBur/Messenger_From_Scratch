"use client";

import { FormEvent, useState } from "react";

type RegisterFormProps = {
  onSubmit: (payload: {
    email: string;
    login: string;
    displayName: string;
    password: string;
  }) => Promise<void>;
  loading: boolean;
};

export default function RegisterForm({
  onSubmit,
  loading,
}: RegisterFormProps) {
  const [email, setEmail] = useState("");
  const [login, setLogin] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await onSubmit({ email, login, displayName, password });
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="form-group">
        <label htmlFor="register-email">Email</label>
        <input
          id="register-email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Enter your email"
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="register-login">Login</label>
        <input
          id="register-login"
          type="text"
          value={login}
          onChange={(e) => setLogin(e.target.value)}
          placeholder="Choose a login"
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="register-display-name">Display name</label>
        <input
          id="register-display-name"
          type="text"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          placeholder="Choose a display name"
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="register-password">Password</label>
        <input
          id="register-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Create a password"
          required
        />
      </div>

      <button className="primary-button" type="submit" disabled={loading}>
        {loading ? "Registering..." : "Create account"}
      </button>
    </form>
  );
}