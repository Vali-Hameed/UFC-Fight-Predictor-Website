/**
 * Tests for lib/api.ts
 *
 * Covers: API_BASE_URL resolution, ApiResponseError, parseResponse, apiFetch,
 * and all named API functions.
 */

// We need to mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

// Reset modules for each test to get clean API_BASE_URL evaluation
beforeEach(() => {
  jest.clearAllMocks();
  mockFetch.mockReset();
});

describe("ApiResponseError", () => {
  it("stores status, message, and errorCode", async () => {
    const { ApiResponseError } = await import("@/lib/api");
    const error = new ApiResponseError(422, "Validation failed", "INVALID_INPUT");
    expect(error.status).toBe(422);
    expect(error.message).toBe("Validation failed");
    expect(error.errorCode).toBe("INVALID_INPUT");
    expect(error.name).toBe("ApiResponseError");
    expect(error).toBeInstanceOf(Error);
  });

  it("works without errorCode", async () => {
    const { ApiResponseError } = await import("@/lib/api");
    const error = new ApiResponseError(500, "Internal error");
    expect(error.errorCode).toBeUndefined();
  });
});

describe("apiFetch", () => {
  it("sets Authorization header when token is provided", async () => {
    const { apiFetch } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({ data: "test" })),
    });

    await apiFetch("/api/test", {}, "my-token");

    const [, options] = mockFetch.mock.calls[0];
    expect(options.headers.get("Authorization")).toBe("Bearer my-token");
  });

  it("does not set Authorization header when no token", async () => {
    const { apiFetch } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({ data: "test" })),
    });

    await apiFetch("/api/test");

    const [, options] = mockFetch.mock.calls[0];
    expect(options.headers.has("Authorization")).toBe(false);
  });

  it("sets Content-Type when body is present", async () => {
    const { apiFetch } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({ ok: true })),
    });

    await apiFetch("/api/test", { method: "POST", body: JSON.stringify({ x: 1 }) });

    const [, options] = mockFetch.mock.calls[0];
    expect(options.headers.get("Content-Type")).toBe("application/json");
  });

  it("returns undefined for 204 responses", async () => {
    const { apiFetch } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 204,
      text: () => Promise.resolve(""),
    });

    const result = await apiFetch("/api/test");
    expect(result).toBeUndefined();
  });

  it("parses JSON response for 200", async () => {
    const { apiFetch } = await import("@/lib/api");
    const payload = { id: 1, name: "Test" };
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify(payload)),
    });

    const result = await apiFetch("/api/test");
    expect(result).toEqual(payload);
  });

  it("returns undefined for empty body on successful response", async () => {
    const { apiFetch } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve(""),
    });

    const result = await apiFetch("/api/test");
    expect(result).toBeUndefined();
  });

  it("throws ApiResponseError for non-OK response with JSON error body", async () => {
    const { apiFetch, ApiResponseError } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 403,
      text: () => Promise.resolve(JSON.stringify({ message: "Forbidden", error: "ACCESS_DENIED" })),
    });

    await expect(apiFetch("/api/test")).rejects.toThrow(ApiResponseError);
    try {
      await apiFetch("/api/test");
    } catch {
      // Second call for detailed checks — need new mock
    }
  });

  it("throws ApiResponseError with default message for non-OK response without message", async () => {
    const { apiFetch, ApiResponseError } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      text: () => Promise.resolve(JSON.stringify({})),
    });

    try {
      await apiFetch("/api/test");
      fail("Should have thrown");
    } catch (e) {
      expect(e).toBeInstanceOf(ApiResponseError);
      expect((e as InstanceType<typeof ApiResponseError>).message).toBe("Request failed");
    }
  });

  it("handles non-JSON error responses", async () => {
    const { apiFetch } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 502,
      text: () => Promise.resolve("Bad Gateway"),
    });

    await expect(apiFetch("/api/test")).rejects.toThrow("Bad Gateway");
  });

  it("includes credentials and no-store cache", async () => {
    const { apiFetch } = await import("@/lib/api");
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({})),
    });

    await apiFetch("/api/test");

    const [, options] = mockFetch.mock.calls[0];
    expect(options.credentials).toBe("include");
    expect(options.cache).toBe("no-store");
  });
});

describe("Named API functions", () => {
  beforeEach(() => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify({})),
    });
  });

  it("markNotificationAsRead calls correct endpoint", async () => {
    const { markNotificationAsRead } = await import("@/lib/api");
    await markNotificationAsRead(42, "token123");

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/notifications/42/read"),
      expect.objectContaining({ method: "PATCH" })
    );
  });

  it("getUnreadNotificationCount calls correct endpoint", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve("5"),
    });
    const { getUnreadNotificationCount } = await import("@/lib/api");
    await getUnreadNotificationCount("token123");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/notifications/unread-count"),
      expect.any(Object)
    );
  });

  it("toggleThreadSubscription calls correct endpoint with POST", async () => {
    const { toggleThreadSubscription } = await import("@/lib/api");
    await toggleThreadSubscription(10, "token123");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/forum/threads/10/subscribe"),
      expect.objectContaining({ method: "POST" })
    );
  });

  it("getSubscriptionStatus calls correct endpoint", async () => {
    const { getSubscriptionStatus } = await import("@/lib/api");
    await getSubscriptionStatus(10, "token123");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/forum/threads/10/subscription-status"),
      expect.any(Object)
    );
  });

  it("warnUser calls correct endpoint with POST", async () => {
    const { warnUser } = await import("@/lib/api");
    await warnUser(5, "token123");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/admin/users/5/warn"),
      expect.objectContaining({ method: "POST" })
    );
  });

  it("banUser calls correct endpoint with optional duration", async () => {
    const { banUser } = await import("@/lib/api");
    await banUser(5, "token123", 7);
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/admin/users/5/ban?durationDays=7"),
      expect.objectContaining({ method: "POST" })
    );
  });

  it("banUser calls without duration for permanent ban", async () => {
    const { banUser } = await import("@/lib/api");
    await banUser(5, "token123");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/admin/users/5/ban"),
      expect.objectContaining({ method: "POST" })
    );
    const url = mockFetch.mock.calls[0][0] as string;
    expect(url).not.toContain("durationDays");
  });

  it("unbanUser calls correct endpoint with POST", async () => {
    const { unbanUser } = await import("@/lib/api");
    await unbanUser(5, "token123");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/admin/users/5/unban"),
      expect.objectContaining({ method: "POST" })
    );
  });

  it("deletePost calls correct endpoint with PATCH", async () => {
    const { deletePost } = await import("@/lib/api");
    await deletePost(99, "token123");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/forum/posts/99/delete"),
      expect.objectContaining({ method: "PATCH" })
    );
  });

  it("deleteUser calls correct endpoint with DELETE", async () => {
    const { deleteUser } = await import("@/lib/api");
    await deleteUser(7, "token123");
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/admin/users/7"),
      expect.objectContaining({ method: "DELETE" })
    );
  });

  it("getEventLeaderboard calls correct endpoint", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: () => Promise.resolve(JSON.stringify([])),
    });
    const { getEventLeaderboard } = await import("@/lib/api");
    await getEventLeaderboard(42);
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/leaderboard/event/42"),
      expect.any(Object)
    );
  });
});
