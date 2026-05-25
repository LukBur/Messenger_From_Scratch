import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import AuthForm from "@/components/AuthForm";

describe("AuthForm", () => {
  it("renders login form by default", () => {
    render(
      <AuthForm
        mode="login"
        setMode={jest.fn()}
        onLogin={jest.fn()}
        onRegister={jest.fn()}
        loading={false}
        message=""
      />,
    );

    expect(
      screen.getByRole("heading", { name: /welcome back/i }),
    ).toBeInTheDocument();

    expect(screen.getByLabelText(/^login$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();

    expect(
      screen.getByRole("button", { name: /^log in$/i }),
    ).toBeInTheDocument();
  });

  it("calls onLogin with login and password", async () => {
    const user = userEvent.setup();
    const onLogin = jest.fn().mockResolvedValue(undefined);

    render(
      <AuthForm
        mode="login"
        setMode={jest.fn()}
        onLogin={onLogin}
        onRegister={jest.fn()}
        loading={false}
        message=""
      />,
    );

    await user.type(screen.getByLabelText(/^login$/i), "janek123");
    await user.type(screen.getByLabelText(/^password$/i), "haslo123");

    await user.click(screen.getByRole("button", { name: /^log in$/i }));

    expect(onLogin).toHaveBeenCalledWith({
      login: "janek123",
      password: "haslo123",
    });
  });

  it("calls setMode when switching to register", async () => {
    const user = userEvent.setup();
    const setMode = jest.fn();

    render(
      <AuthForm
        mode="login"
        setMode={setMode}
        onLogin={jest.fn()}
        onRegister={jest.fn()}
        loading={false}
        message=""
      />,
    );

    await user.click(screen.getByRole("button", { name: /^register$/i }));

    expect(setMode).toHaveBeenCalledWith("register");
  });
});
