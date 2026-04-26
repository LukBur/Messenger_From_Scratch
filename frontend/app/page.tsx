"use client";

import { useEffect, useState } from "react";

type UserResponse = {
  id: string;
  email: string;
  login: string;
  displayName: string;
  role: string;
  avatarUrl: string | null;
};

export default function HomePage() {
  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [token, setToken] = useState<string | null>(null);
  const [currentUser, setCurrentUser] = useState<UserResponse | null>(null);
  const [loadingUser, setLoadingUser] = useState(false);

  const fetchCurrentUser = async (jwt: string) => {
    try {
      setLoadingUser(true);

      const response = await fetch("http://localhost:8080/api/users/me", {
        headers: {
          Authorization: `Bearer ${jwt}`,
        },
      });

      if (!response.ok) {
        localStorage.removeItem("token");
        setToken(null);
        setCurrentUser(null);
        setMessage("Session expired or invalid token");
        return;
      }

      const data = await response.json();
      setCurrentUser(data);
    } catch {
      setMessage("Could not load user data");
    } finally {
      setLoadingUser(false);
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage("");

    try {
      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          login,
          password,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setMessage(data.message || "Login failed");
        return;
      }

      localStorage.setItem("token", data.token);
      setToken(data.token);
      await fetchCurrentUser(data.token);
      setMessage("Login successful");
    } catch {
      setMessage("Server connection error");
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    setToken(null);
    setCurrentUser(null);
    setLogin("");
    setPassword("");
    setMessage("Logged out");
  };

  useEffect(() => {
    const savedToken = localStorage.getItem("token");

    if (savedToken) {
      setToken(savedToken);
      fetchCurrentUser(savedToken);
    }
  }, []);

  if (loadingUser) {
    return (
      <main style={{ padding: "2rem" }}>
        <p>Loading user...</p>
      </main>
    );
  }

  if (currentUser) {
    return (
      <main style={{ padding: "2rem" }}>
        <h1>Messenger</h1>
        <p>Logged in as: <strong>{currentUser.displayName}</strong></p>
        <p>Login: {currentUser.login}</p>
        <p>Email: {currentUser.email}</p>
        <p>Role: {currentUser.role}</p>

        <button onClick={handleLogout} style={{ marginTop: "1rem" }}>
          Log out
        </button>

        {message && <p style={{ marginTop: "1rem" }}>{message}</p>}
      </main>
    );
  }

  return (
    <main style={{ padding: "2rem" }}>
      <h1>Login</h1>

      <form
        onSubmit={handleLogin}
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "1rem",
          maxWidth: "320px",
        }}
      >
        <input
          type="text"
          placeholder="Login"
          value={login}
          onChange={(e) => setLogin(e.target.value)}
        />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <button type="submit">Log in</button>
      </form>

      {message && <p style={{ marginTop: "1rem" }}>{message}</p>}
    </main>
  );
}