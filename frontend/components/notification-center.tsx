/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { apiFetch, NotificationDto, markAllNotificationsAsRead, deleteNotification, deleteAllNotifications, deleteBatchNotifications } from "@/lib/api";
import { useAuth } from "@/lib/session";
import { toast } from "sonner";
import Link from "next/link";

const CheckIcon = ({ className }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className={className}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
  </svg>
);

const TrashIcon = ({ className }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className={className}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
  </svg>
);

export function NotificationCenter() {
  const { token } = useAuth();
  const [items, setItems] = useState<NotificationDto[]>([]);
  const [selectionMode, setSelectionMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const holdTimer = useRef<NodeJS.Timeout | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (selectionMode && containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setSelectionMode(false);
        setSelectedIds(new Set());
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [selectionMode]);

  useEffect(() => {
    if (!token) {
      return;
    }

    void apiFetch<NotificationDto[]>("/api/v1/notifications", {}, token)
      .then(setItems)
      .catch(() => toast.error("Could not load notifications."));
  }, [token]);

  const dispatchUpdate = () => window.dispatchEvent(new Event("notificationsUpdated"));

  const markRead = async (id: number) => {
    if (!token) return;
    await apiFetch(`/api/v1/notifications/${id}/read`, { method: "PATCH" }, token);
    setItems((current) => current.map((item) => item.id === id ? { ...item, read: true } : item));
    dispatchUpdate();
  };

  const markAllRead = async () => {
    if (!token) return;
    try {
      await markAllNotificationsAsRead(token);
      setItems((current) => current.map((item) => ({ ...item, read: true })));
      toast.success("All notifications marked as read.");
      dispatchUpdate();
    } catch {
      toast.error("Failed to mark all as read.");
    }
  };

  const handleDelete = async (id: number, e?: React.MouseEvent) => {
    e?.preventDefault();
    e?.stopPropagation();
    if (!token) return;
    try {
      await deleteNotification(id, token);
      setItems((current) => current.filter((item) => item.id !== id));
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
      toast.success("Notification deleted.");
      dispatchUpdate();
    } catch {
      toast.error("Failed to delete notification.");
    }
  };

  const handleDeleteAll = async () => {
    if (!token) return;
    try {
      await deleteAllNotifications(token);
      setItems([]);
      setSelectedIds(new Set());
      setSelectionMode(false);
      toast.success("All notifications deleted.");
      dispatchUpdate();
    } catch {
      toast.error("Failed to delete all notifications.");
    }
  };

  const handleDeleteSelected = async () => {
    if (!token || selectedIds.size === 0) return;
    try {
      await deleteBatchNotifications(Array.from(selectedIds), token);
      setItems((current) => current.filter((item) => !selectedIds.has(item.id)));
      setSelectedIds(new Set());
      setSelectionMode(false);
      toast.success("Selected notifications deleted.");
      dispatchUpdate();
    } catch {
      toast.error("Failed to delete selected notifications.");
    }
  };

  const startHold = (id: number) => {
    if (holdTimer.current) clearTimeout(holdTimer.current);
    holdTimer.current = setTimeout(() => {
      setSelectionMode(true);
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.add(id);
        return next;
      });
      if ("vibrate" in navigator) navigator.vibrate(50);
    }, 500); // 500ms long press
  };

  const cancelHold = () => {
    if (holdTimer.current) {
      clearTimeout(holdTimer.current);
      holdTimer.current = null;
    }
  };

  const toggleSelection = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  if (!token) {
    return <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Sign in to view notifications.</div>;
  }

  const hasUnread = items.some((item) => !item.read);
  const hasItems = items.length > 0;

  return (
    <div ref={containerRef} className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex gap-2 items-center">
          {selectionMode && (
             <button 
                onClick={() => setSelectionMode(false)}
                className="text-xs text-white/70 hover:text-white"
             >
                Cancel Selection
             </button>
          )}
        </div>
        <div className="flex gap-2">
          {selectionMode && selectedIds.size > 0 && (
            <button 
              onClick={() => void handleDeleteSelected()} 
              className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-2 text-sm font-semibold text-red-500 transition hover:bg-red-500/20"
            >
              Delete Selected ({selectedIds.size})
            </button>
          )}
          {!selectionMode && hasUnread && (
            <button 
              onClick={() => void markAllRead()} 
              className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/10"
            >
              Mark all as read
            </button>
          )}
          {!selectionMode && hasItems && (
            <button 
              onClick={() => void handleDeleteAll()} 
              className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-2 text-sm font-semibold text-red-500 transition hover:bg-red-500/20"
            >
              Delete All
            </button>
          )}
        </div>
      </div>
      <div className="space-y-3 relative">
        {items.map((item) => (
          <div 
            key={item.id} 
            className={`rounded-2xl border border-white/10 p-4 transition-colors select-none ${selectionMode && selectedIds.has(item.id) ? 'bg-red-500/10 border-red-500/30' : 'bg-white/5'}`}
            onPointerDown={() => !selectionMode && startHold(item.id)}
            onPointerUp={cancelHold}
            onPointerLeave={cancelHold}
            onPointerCancel={cancelHold}
            onClick={() => {
              if (selectionMode) {
                toggleSelection(item.id);
              }
            }}
          >
            <div className="flex items-start gap-3">
              {selectionMode && (
                <div className="flex-shrink-0 mt-1">
                  <div className={`w-5 h-5 rounded border flex items-center justify-center transition-colors ${selectedIds.has(item.id) ? 'bg-red-500 border-red-500' : 'border-white/30'}`}>
                    {selectedIds.has(item.id) && <CheckIcon className="w-3.5 h-3.5 text-white" />}
                  </div>
                </div>
              )}
              <div className="flex-1">
                <p className="text-xs uppercase tracking-[0.3em] text-gold">{item.type ?? "Notification"}</p>
                {item.link ? (
                  <Link href={item.link as any} onClick={(e) => { 
                      if (selectionMode) { e.preventDefault(); return; }
                      if (!item.read) markRead(item.id); 
                    }} 
                    className="mt-2 text-sm text-white hover:text-accent transition block"
                  >
                    {item.message}
                  </Link>
                ) : (
                  <p className="mt-2 text-sm text-white/75">{item.message}</p>
                )}
              </div>
              <div className="flex flex-col gap-2 items-end">
                <button onClick={(e) => {
                  if (selectionMode) { e.stopPropagation(); return; }
                  void markRead(item.id);
                }} className="rounded-full border border-white/10 bg-bg/70 px-3 py-1 text-xs text-white/70 hover:bg-white/10 transition z-10 relative">
                  {item.read ? "Read" : "Mark read"}
                </button>
                <button onClick={(e) => handleDelete(item.id, e)} className="rounded-full border border-red-500/20 bg-red-500/10 p-1.5 text-red-500 hover:bg-red-500/20 transition z-10 relative" title="Delete notification">
                  <TrashIcon className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        ))}
        {items.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/60">No notifications yet.</div> : null}
      </div>
    </div>
  );
}