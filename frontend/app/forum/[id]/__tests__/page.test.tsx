/**
 * Tests for app/forum/[id]/page.tsx (server component)
 */

import { render, screen } from "@testing-library/react";

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

jest.mock("@/components/forum-reply-form", () => ({
  ForumReplyForm: ({ threadId }: any) => <div data-testid="reply-form">Reply to {threadId}</div>,
}));

jest.mock("@/components/subscribe-button", () => ({
  SubscribeButton: ({ threadId }: any) => <button data-testid="subscribe-btn">Sub {threadId}</button>,
}));

jest.mock("@/components/admin-moderation-menu", () => ({
  AdminModerationMenu: () => <div data-testid="mod-menu">Mod</div>,
}));

async function renderForumThread(id: string) {
  const ForumThreadPage = (await import("@/app/forum/[id]/page")).default;
  const jsx = await ForumThreadPage({ params: Promise.resolve({ id }) });
  return render(jsx as any);
}

beforeEach(() => {
  jest.clearAllMocks();
  mockApiFetch.mockResolvedValue([]);
});

describe("ForumThreadPage", () => {
  it("shows error for invalid thread ID", async () => {
    await renderForumThread("abc");
    expect(screen.getByText("Thread not found")).toBeInTheDocument();
  });

  it("renders thread with posts", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ id: 5, title: "Fight Discussion", eventId: 1, fightId: 10, createdBy: 1, createdAt: null }) // thread
      .mockResolvedValueOnce([ // posts
        { id: 1, threadId: 5, userId: 1, username: "john", content: "Great fight!", createdAt: "2026-01-01T00:00:00Z", updatedAt: null, isDeleted: false },
      ])
      .mockResolvedValueOnce({ predictedWinner: "Fighter A", confidenceScore: 0.85 }); // ml prediction

    await renderForumThread("5");

    expect(screen.getByText("Fight Discussion")).toBeInTheDocument();
    expect(screen.getByText("Great fight!")).toBeInTheDocument();
    expect(screen.getByText("john")).toBeInTheDocument();
  });

  it("renders subscribe button and reply form when thread exists", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ id: 5, title: "Thread", eventId: 1, fightId: null, createdBy: 1, createdAt: null })
      .mockResolvedValueOnce([]);

    await renderForumThread("5");

    expect(screen.getByTestId("subscribe-btn")).toBeInTheDocument();
    expect(screen.getByTestId("reply-form")).toBeInTheDocument();
  });

  it("shows empty posts message", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ id: 5, title: "Thread", eventId: 1, fightId: null, createdBy: 1, createdAt: null })
      .mockResolvedValueOnce([]);

    await renderForumThread("5");

    expect(screen.getByText("No replies yet. Be the first to respond.")).toBeInTheDocument();
  });

  it("renders deleted post label", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ id: 5, title: "Thread", eventId: 1, fightId: null, createdBy: 1, createdAt: null })
      .mockResolvedValueOnce([
        { id: 1, threadId: 5, userId: 1, username: "user", content: "deleted content", createdAt: null, updatedAt: null, isDeleted: true },
      ]);

    await renderForumThread("5");
    expect(screen.getByText("Deleted")).toBeInTheDocument();
  });

  it("highlights @mentions in post content", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ id: 5, title: "Thread", eventId: 1, fightId: null, createdBy: 1, createdAt: null })
      .mockResolvedValueOnce([
        { id: 1, threadId: 5, userId: 1, username: "user", content: "Hey @john check this out", createdAt: null, updatedAt: null, isDeleted: false },
      ]);

    await renderForumThread("5");
    expect(screen.getByText("@john")).toBeInTheDocument();
  });

  it("renders ML prediction for fight thread", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ id: 5, title: "Thread", eventId: 1, fightId: 10, createdBy: 1, createdAt: null })
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce({ predictedWinner: "Champ", confidenceScore: 0.92, cachedAt: null });

    await renderForumThread("5");
    expect(screen.getByText(/Champ/)).toBeInTheDocument();
    expect(screen.getByText(/92%/)).toBeInTheDocument();
  });
});
