import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PasswordInput } from "@/components/password-input";

describe("PasswordInput", () => {
  it("renders with password type by default", () => {
    render(<PasswordInput name="pwd" />);
    const input = screen.getByPlaceholderText("Password");
    expect(input).toHaveAttribute("type", "password");
    expect(input).toHaveAttribute("name", "pwd");
  });

  it("uses custom placeholder", () => {
    render(<PasswordInput name="pwd" placeholder="Enter secret" />);
    expect(screen.getByPlaceholderText("Enter secret")).toBeInTheDocument();
  });

  it("toggles visibility on button click", async () => {
    const user = userEvent.setup();
    render(<PasswordInput name="pwd" />);
    const input = screen.getByPlaceholderText("Password");
    const toggleBtn = screen.getByRole("button");

    expect(input).toHaveAttribute("type", "password");

    await user.click(toggleBtn);
    expect(input).toHaveAttribute("type", "text");

    await user.click(toggleBtn);
    expect(input).toHaveAttribute("type", "password");
  });

  it("has required attribute", () => {
    render(<PasswordInput name="pwd" />);
    expect(screen.getByPlaceholderText("Password")).toBeRequired();
  });
});
