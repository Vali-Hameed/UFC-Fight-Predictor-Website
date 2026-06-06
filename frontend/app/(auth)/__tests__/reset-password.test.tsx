import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ResetPasswordPage from "@/app/(auth)/reset-password/page";
import { toast } from "sonner";

jest.mock("sonner");

const mockRouter = { push: jest.fn(), refresh: jest.fn() };
let mockSearchParams = new URLSearchParams();
jest.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
  useSearchParams: () => mockSearchParams,
}));

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockSearchParams = new URLSearchParams("?token=reset-abc123");
});

describe("ResetPasswordPage", () => {
  it("renders form with password fields", () => {
    render(<ResetPasswordPage />);
    expect(screen.getByText("Reset password")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New password")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Confirm new password")).toBeInTheDocument();
  });

  it("shows error toast for password mismatch", async () => {
    const user = userEvent.setup();
    render(<ResetPasswordPage />);

    await user.type(screen.getByPlaceholderText("New password"), "pass1");
    await user.type(screen.getByPlaceholderText("Confirm new password"), "pass2");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(toast.error).toHaveBeenCalledWith("Passwords do not match.");
  });

  it("shows error for missing token", async () => {
    mockSearchParams = new URLSearchParams("");

    const user = userEvent.setup();
    render(<ResetPasswordPage />);

    // Wait for useEffect to run and clear tokenFromQuery
    await waitFor(() => {});

    await user.type(screen.getByPlaceholderText("New password"), "pass1");
    await user.type(screen.getByPlaceholderText("Confirm new password"), "pass1");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    expect(toast.error).toHaveBeenCalledWith("Invalid or missing reset token.");
  });

  it("successfully resets password", async () => {
    mockApiFetch.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(<ResetPasswordPage />);

    await user.type(screen.getByPlaceholderText("New password"), "newpass123");
    await user.type(screen.getByPlaceholderText("Confirm new password"), "newpass123");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith("Password updated successfully. You will be redirected to login.");
    });
  });

  it("shows error toast on reset failure", async () => {
    mockApiFetch.mockRejectedValueOnce(new Error("Token expired"));
    const user = userEvent.setup();

    render(<ResetPasswordPage />);

    await user.type(screen.getByPlaceholderText("New password"), "pass");
    await user.type(screen.getByPlaceholderText("Confirm new password"), "pass");
    await user.click(screen.getByRole("button", { name: "Update password" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Reset failed. The token may be expired or invalid.");
    });
  });
});
