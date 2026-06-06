import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ForgotPasswordPage from "@/app/(auth)/forgot-password/page";
import { toast } from "sonner";

jest.mock("sonner");

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe("ForgotPasswordPage", () => {
  it("renders email input and submit button", () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByText("Forgot password")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Email for reset link")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Request reset link" })).toBeInTheDocument();
  });

  it("renders sign in link", () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByText("Sign in")).toBeInTheDocument();
  });

  it("shows success toast on submit", async () => {
    mockApiFetch.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(<ForgotPasswordPage />);
    await user.type(screen.getByPlaceholderText("Email for reset link"), "test@example.com");
    await user.click(screen.getByRole("button", { name: "Request reset link" }));

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith("Reset link requested. Check your email inbox.");
    });
  });

  it("shows error toast on failure", async () => {
    mockApiFetch.mockRejectedValueOnce(new Error("Failed"));
    const user = userEvent.setup();

    render(<ForgotPasswordPage />);
    await user.type(screen.getByPlaceholderText("Email for reset link"), "test@example.com");
    await user.click(screen.getByRole("button", { name: "Request reset link" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Could not request a reset link.");
    });
  });
});
