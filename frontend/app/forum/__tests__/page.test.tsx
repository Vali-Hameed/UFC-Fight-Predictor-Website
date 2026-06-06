import ForumPage from "@/app/forum/page";

const mockRedirect = jest.fn();
jest.mock("next/navigation", () => ({
  redirect: (...args: unknown[]) => mockRedirect(...args),
}));

describe("ForumPage", () => {
  it("redirects to /events", () => {
    ForumPage();
    expect(mockRedirect).toHaveBeenCalledWith("/events");
  });
});
