import { render, screen } from "@testing-library/react";
import NotificationsPage from "@/app/notifications/page";

jest.mock("@/components/notification-center", () => ({
  NotificationCenter: () => <div data-testid="notification-center">Mocked NotificationCenter</div>,
}));

describe("NotificationsPage", () => {
  it("renders SectionCard with NotificationCenter", () => {
    render(<NotificationsPage />);
    expect(screen.getByText("Notifications")).toBeInTheDocument();
    expect(screen.getByTestId("notification-center")).toBeInTheDocument();
  });

  it("renders correct eyebrow and description", () => {
    render(<NotificationsPage />);
    expect(screen.getByText("Inbox")).toBeInTheDocument();
    expect(screen.getByText(/Result notifications, reminders/)).toBeInTheDocument();
  });
});
