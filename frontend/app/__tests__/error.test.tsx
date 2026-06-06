import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ErrorPage from "@/app/error";

describe("ErrorPage", () => {
  it("renders 500 label and error message", () => {
    render(<ErrorPage reset={() => {}} />);
    expect(screen.getByText("500")).toBeInTheDocument();
    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
    expect(screen.getByText(/The page hit an unexpected problem/)).toBeInTheDocument();
  });

  it("calls reset when Retry button is clicked", async () => {
    const mockReset = jest.fn();
    const user = userEvent.setup();
    render(<ErrorPage reset={mockReset} />);

    await user.click(screen.getByText("Retry"));
    expect(mockReset).toHaveBeenCalledTimes(1);
  });
});
