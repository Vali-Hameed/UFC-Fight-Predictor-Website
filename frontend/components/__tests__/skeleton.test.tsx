import { render } from "@testing-library/react";
import { Skeleton } from "@/components/ui/skeleton";

describe("Skeleton", () => {
  it("renders with animate-pulse class", () => {
    const { container } = render(<Skeleton />);
    const el = container.firstChild as HTMLElement;
    expect(el.className).toContain("animate-pulse");
    expect(el.className).toContain("rounded-md");
    expect(el.className).toContain("bg-white/10");
  });

  it("merges custom className", () => {
    const { container } = render(<Skeleton className="h-6 w-32" />);
    const el = container.firstChild as HTMLElement;
    expect(el.className).toContain("animate-pulse");
    expect(el.className).toContain("h-6 w-32");
  });

  it("passes through extra props", () => {
    const { container } = render(<Skeleton data-testid="my-skeleton" />);
    const el = container.querySelector('[data-testid="my-skeleton"]');
    expect(el).toBeInTheDocument();
  });
});
