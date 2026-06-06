import React from "react";

export const motion = {
  div: React.forwardRef(
    ({ children, ...props }: React.PropsWithChildren<Record<string, unknown>>, ref: React.Ref<HTMLDivElement>) => (
      <div ref={ref} {...filterMotionProps(props)}>
        {children}
      </div>
    )
  ),
};

function filterMotionProps(props: Record<string, unknown>): Record<string, unknown> {
  const motionKeys = ["initial", "animate", "exit", "transition", "variants", "whileHover", "whileTap", "whileInView"];
  const filtered: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(props)) {
    if (!motionKeys.includes(key)) {
      filtered[key] = value;
    }
  }
  return filtered;
}

export const AnimatePresence = ({ children }: React.PropsWithChildren) => <>{children}</>;
