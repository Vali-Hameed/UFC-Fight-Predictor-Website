"use client";

import { useEffect, useState } from "react";
import { apiFetch, NotificationDto } from "@/lib/api";
import { useAuth } from "@/lib/session";

export function NotificationCenter() {
  const { token } = useAuth();
  const [items, setItems] = useState<NotificationDto[]>([]);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }

    void apiFetch<NotificationDto[]>("/api/v1/notifications", {}, token)
      .then(setItems)
      .catch(() => setMessage("Could not load notifications."));
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
      {message ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-red-200">{message}</div> : null}
      {items.map((item) => (
        <div key={item.id} className="rounded-2xl border border-white/10 bg-white/5 p-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-gold">{item.type ?? "Notification"}</p>
              <p className="mt-2 text-sm text-white/75">{item.message}</p>
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