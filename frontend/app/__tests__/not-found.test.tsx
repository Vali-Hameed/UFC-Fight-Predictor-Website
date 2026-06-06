import { render, screen } from "@testing-library/react";
import NotFound from "@/app/not-found";

describe("NotFound", () => {
  it("renders 404 label and message", () => {
    render(<NotFound />);
    expect(screen.getByText("404")).toBeInTheDocument();
    expect(screen.getByText("Page not found")).toBeInTheDocument();
    expect(screen.getByText(/The requested page does not exist/)).toBeInTheDocument();
  });

  it("renders Back home link pointing to /", () => {
    render(<NotFound />);
    const link = screen.getByText("Back home");
    expect(link.closest("a")).toHaveAttribute("href", "/");
  });
});
