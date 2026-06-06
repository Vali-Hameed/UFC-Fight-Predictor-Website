import { render } from "@testing-library/react";
import AdminLoading from "@/app/admin/loading";

describe("AdminLoading", () => {
  it("renders skeleton placeholders", () => {
    const { container } = render(<AdminLoading />);
    const skeletons = container.querySelectorAll(".animate-pulse");
    expect(skeletons.length).toBeGreaterThan(5);
  });

  it("renders section cards", () => {
    const { getByText } = render(<AdminLoading />);
    expect(getByText("Operations panel")).toBeInTheDocument();
    expect(getByText("Moderation tools")).toBeInTheDocument();
  });
});
