/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { useEffect, useState } from "react";
import { apiFetch, NotificationDto } from "@/lib/api";
import { useAuth } from "@/lib/session";
import { toast } from "sonner";
import Link from "next/link";

export function NotificationCenter() {
  const { token } = useAuth();
  const [items, setItems] = useState<NotificationDto[]>([]);

  useEffect(() => {
    if (!token) {
      return;
    }

    void apiFetch<NotificationDto[]>("/api/v1/notifications", {}, token)
      .then(setItems)
      .catch(() => toast.error("Could not load notifications."));
  }, [token]);

  const markRead = async (id: number) => {
    if (!token) return;
    await apiFetch(`/api/v1/notifications/${id}/read`, { method: "PATCH" }, token);
    setItems((current) => current.map((item) => item.id === id ? { ...item, read: true } : item));
  };

  if (!token) {
    return <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Sign in to view notifications.</div>;
  }

  return (
    <div className="space-y-3">
      {items.map((item) => (
        <div key={item.id} className="rounded-2xl border border-white/10 bg-white/5 p-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-gold">{item.type ?? "Notification"}</p>
              {item.link ? (
                <Link href={item.link as any} onClick={() => { if (!item.read) markRead(item.id); }} className="mt-2 text-sm text-white hover:text-accent transition block">
                  {item.message}
                </Link>
              ) : (
                <p className="mt-2 text-sm text-white/75">{item.message}</p>
              )}
            </div>
            <button onClick={() => void markRead(item.id)} className="rounded-full border border-white/10 bg-bg/70 px-3 py-1 text-xs text-white/70">
              {item.read ? "Read" : "Mark read"}
            </button>
          </div>
        </div>
      ))}
      {items.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/60">No notifications yet.</div> : null}
    </div>
  );
}