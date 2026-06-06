import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ForumReplyForm } from "@/components/forum-reply-form";
import { toast } from "sonner";

jest.mock("sonner");

const mockRouter = { push: jest.fn(), refresh: jest.fn() };
jest.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
}));

const mockToken = { current: null as string | null };
jest.mock("@/lib/session", () => ({
  useAuth: () => ({ token: mockToken.current }),
}));

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockToken.current = null;
});

describe("ForumReplyForm", () => {
  it("shows sign-in message when no token", () => {
    render(<ForumReplyForm threadId={1} />);
    expect(screen.getByText("Sign in to reply to this thread.")).toBeInTheDocument();
  });

  it("renders form when authenticated", () => {
    mockToken.current = "token123";
    render(<ForumReplyForm threadId={1} />);
    expect(screen.getByPlaceholderText("Write a reply...")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Post reply" })).toBeInTheDocument();
  });

  it("disables submit when content is empty", () => {
    mockToken.current = "token123";
    render(<ForumReplyForm threadId={1} />);
    expect(screen.getByRole("button", { name: "Post reply" })).toBeDisabled();
  });

  it("enables submit when content is entered", async () => {
    mockToken.current = "token123";
    const user = userEvent.setup();
    render(<ForumReplyForm threadId={1} />);

    await user.type(screen.getByPlaceholderText("Write a reply..."), "Great fight!");
    expect(screen.getByRole("button", { name: "Post reply" })).not.toBeDisabled();
  });

  it("submits reply successfully", async () => {
    mockToken.current = "token123";
    mockApiFetch.mockResolvedValueOnce({});
    const user = userEvent.setup();

    render(<ForumReplyForm threadId={5} />);

    await user.type(screen.getByPlaceholderText("Write a reply..."), "My prediction is...");
    await user.click(screen.getByRole("button", { name: "Post reply" }));

    await waitFor(() => {
      expect(mockApiFetch).toHaveBeenCalledWith(
        "/api/v1/forum/posts",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ threadId: 5, content: "My prediction is..." }),
        }),
        "token123"
      );
      expect(toast.success).toHaveBeenCalledWith("Reply posted.");
      expect(mockRouter.refresh).toHaveBeenCalled();
    });
  });

  it("clears textarea after successful submission", async () => {
    mockToken.current = "token123";
    mockApiFetch.mockResolvedValueOnce({});
    const user = userEvent.setup();

    render(<ForumReplyForm threadId={5} />);

    const textarea = screen.getByPlaceholderText("Write a reply...");
    await user.type(textarea, "Hello");
    await user.click(screen.getByRole("button", { name: "Post reply" }));

    await waitFor(() => {
      expect(textarea).toHaveValue("");
    });
  });

  it("shows error toast on failed submission", async () => {
    mockToken.current = "token123";
    mockApiFetch.mockRejectedValueOnce(new Error("Server error"));
    const user = userEvent.setup();

    render(<ForumReplyForm threadId={5} />);

    await user.type(screen.getByPlaceholderText("Write a reply..."), "Test");
    await user.click(screen.getByRole("button", { name: "Post reply" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Could not post reply.");
    });
  });
});
