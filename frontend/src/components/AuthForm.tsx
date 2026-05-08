"use client";

import LoginForm from "@/components/LoginForm";
import RegisterForm from "@/components/RegisterForm";

type AuthMode = "login" | "register";

type AuthFormProps = {
  mode: AuthMode;
  setMode: (mode: AuthMode) => void;
  onLogin: (payload: { login: string; password: string }) => Promise<void>;
  onRegister: (payload: {
    email: string;
    login: string;
    displayName: string;
    password: string;
  }) => Promise<void>;
  loading: boolean;
  message: string;
};

export default function AuthForm({
  mode,
  setMode,
  onLogin,
  onRegister,
  loading,
  message,
}: AuthFormProps) {
  return (
    <section className="auth-shell">
      <div className="auth-card">
        <div className="auth-intro">
          <p className="eyebrow">Best Communicator</p>
          <h1>{mode === "login" ? "Welcome back" : "Create your account"}</h1>
          <p className="muted-text">
            {mode === "login"
              ? "Log in to continue to your conversations."
              : "Register to start using the communicator."}
          </p>
        </div>

        <div className="auth-switch">
          <button
            className={mode === "login" ? "tab-button active" : "tab-button"}
            onClick={() => setMode("login")}
            type="button"
          >
            Login
          </button>
          <button
            className={mode === "register" ? "tab-button active" : "tab-button"}
            onClick={() => setMode("register")}
            type="button"
          >
            Register
          </button>
        </div>

        {mode === "login" ? (
          <LoginForm onSubmit={onLogin} loading={loading} />
        ) : (
          <RegisterForm onSubmit={onRegister} loading={loading} />
        )}

        {message && <p className="status-message">{message}</p>}
      </div>
    </section>
  );
}