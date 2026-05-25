import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import MessageComposer from "@/components/MessageComposer";

describe("MessageComposer", () => {
  it("calls onSendMessage with typed message", async () => {
    const user = userEvent.setup();
    const onSendMessage = jest.fn().mockResolvedValue(undefined);

    render(<MessageComposer onSendMessage={onSendMessage} loading={false} />);

    const input = screen.getByRole("textbox");
    const button = screen.getByRole("button", { name: /send/i });

    await user.type(input, "Hello world");
    await user.click(button);

    expect(onSendMessage).toHaveBeenCalledWith("Hello world", undefined);
  });

  it("does not send empty message", async () => {
    const user = userEvent.setup();
    const onSendMessage = jest.fn().mockResolvedValue(undefined);

    render(<MessageComposer onSendMessage={onSendMessage} loading={false} />);

    const button = screen.getByRole("button", { name: /send/i });
    await user.click(button);

    expect(onSendMessage).not.toHaveBeenCalled();
  });
});
