"use client";

import { useEffect, useState } from "react";
import { formatEventDate } from "@/lib/api";

export function LocalTime({ dateStr }: { dateStr: string | null }) {
  const [local, setLocal] = useState<string | null>(null);

  useEffect(() => {
    setLocal(formatEventDate(dateStr));
  }, [dateStr]);

  if (!local) return null;
  return <span suppressHydrationWarning>{local}</span>;
}
