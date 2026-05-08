"use client";

import { useEffect, useState } from "react";
import AuthForm from "@/components/AuthForm";
import UserPanel from "@/components/UserPanel";
import { getCurrentUser, loginUser, registerUser } from "@/lib/api";
import { UserResponse } from "@/types/user";

type AuthMode = "login" | "register";

export default function HomePage() {
  const [mode, setMode] = useState<AuthMode>("login");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingUser, setLoadingUser] = useState(true);
  const [currentUser, setCurrentUser] = useState<UserResponse | null>(null);

  const fetchCurrentUser = async (token: string) => {
    try {
      setLoadingUser(true);
      const user = await getCurrentUser(token);
      setCurrentUser(user);
      setMessage("");
    } catch (error) {
      localStorage.removeItem("token");
      setCurrentUser(null);
      setMessage(
        error instanceof Error ? error.message : "Could not load user data"
      );
    } finally {
      setLoadingUser(false);
    }
  };

  const handleLogin = async (payload: {
    login: string;
    password: string;
  }) => {
    try {
      setLoading(true);
      setMessage("");

      const data = await loginUser(payload);
      localStorage.setItem("token", data.token);

      await fetchCurrentUser(data.token);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Login failed");
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (payload: {
    email: string;
    login: string;
    displayName: string;
    password: string;
  }) => {
    try {
      setLoading(true);
      setMessage("");

      const data = await registerUser(payload);
      setMessage(data.message || "Registration successful");
      setMode("login");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    setCurrentUser(null);
    setMessage("Logged out");
    setMode("login");
  };

  useEffect(() => {
    const savedToken = localStorage.getItem("token");

    if (!savedToken) {
      setLoadingUser(false);
      return;
    }

    fetchCurrentUser(savedToken);
  }, []);

  if (loadingUser) {
    return (
      <main className="page-wrapper">
        <div className="center-box">
          <p>Loading...</p>
        </div>
      </main>
    );
  }

  return (
    <main className="page-wrapper">
      <div className="page-container">
        {currentUser ? (
          <UserPanel user={currentUser} onLogout={handleLogout} />
        ) : (
          <AuthForm
            mode={mode}
            setMode={setMode}
            onLogin={handleLogin}
            onRegister={handleRegister}
            loading={loading}
            message={message}
          />
        )}
      </div>
    </main>
  );
}