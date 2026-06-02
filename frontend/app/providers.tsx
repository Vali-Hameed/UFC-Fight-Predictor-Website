"use client";

import { AuthProvider } from "@/lib/session";
import type { ReactNode } from "react";
import { Toaster } from "sonner";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      {children}
      <Toaster position="bottom-right" theme="dark" richColors />
    </AuthProvider>
  );
}