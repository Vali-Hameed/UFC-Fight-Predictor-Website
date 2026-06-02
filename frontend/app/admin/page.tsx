"use client";

import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { AdminUserDto, apiFetch, ScrapeLogDto } from "@/lib/api";
import { useAuth } from "@/lib/session";
import { notFound } from "next/navigation";
import { toast } from "sonner";

export default function AdminPage() {
  const { token, loading, user } = useAuth();
  const [logs, setLogs] = useState<ScrapeLogDto[]>([]);
  const [users, setUsers] = useState<AdminUserDto[]>([]);
  const [roleDrafts, setRoleDrafts] = useState<Record<number, string>>({});



  const loadUsers = async () => {
    if (!token) return;
    const items = await apiFetch<AdminUserDto[]>("/api/v1/admin/users", {}, token);
    setUsers(items);
    setRoleDrafts(Object.fromEntries(items.map((item) => [item.id, item.role?.name ?? ""])));
  };

  const loadLogs = async () => {
    if (!token) return;
    const items = await apiFetch<ScrapeLogDto[]>("/api/v1/internal/scraper/logs", {}, token);
    setLogs(items);
  };

  useEffect(() => {
    if (!token) return;

    void loadUsers().catch(() => toast.error("Could not load users."));
    void loadLogs().catch(() => toast.error("Could not load scraper logs."));
  }, [token]);

  const triggerPrewarm = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) return;
    try {
      await apiFetch("/api/v1/admin/prewarm/trigger", { method: "POST" }, token);
      toast.success("Prewarm triggered.");
    } catch {
      toast.error("Could not trigger prewarm.");
    }
  };

  const triggerScraper = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) return;
    try {
      await apiFetch("/api/v1/admin/scraper/trigger", { method: "POST" }, token);
      toast.success("Scraper triggered successfully. This may take a few minutes.");
    } catch {
      toast.error("Could not trigger scraper. Check backend logs.");
    }
  };

  const saveRole = async (userId: number) => {
    if (!token) return;
    const role = roleDrafts[userId]?.trim();
    if (!role) {
      toast.error("Role cannot be empty.");
      return;
    }

    try {
      const updatedUser = await apiFetch<AdminUserDto>(
        `/api/v1/admin/users/${userId}/role`,
        {
          method: "PATCH",
          body: JSON.stringify({ role })
        },
        token
      );
      setUsers((current) => current.map((user) => (user.id === userId ? updatedUser : user)));
      toast.success(`Updated role for ${updatedUser.username ?? `user #${userId}`}.`);
    } catch {
      toast.error("Could not update role.");
    }
  };

  const toggleLock = async (userId: number, locked: boolean) => {
    if (!token) return;

    try {
      const updatedUser = await apiFetch<AdminUserDto>(
        `/api/v1/admin/users/${userId}/ban?locked=${locked}`,
        { method: "PATCH" },
        token
      );
      setUsers((current) => current.map((user) => (user.id === userId ? updatedUser : user)));
      toast.success(`${updatedUser.username ?? `user #${userId}`} ${locked ? "locked" : "unlocked"}.`);
    } catch {
      toast.error("Could not update lock state.");
    }
  };

  const handleRoleChange = (userId: number, event: ChangeEvent<HTMLInputElement>) => {
    setRoleDrafts((current) => ({
      ...current,
      [userId]: event.target.value
    }));
  };

  if (!loading && (!user || user.role !== "ROLE_ADMIN")) {
    notFound();
  }

  if (loading) {
    return null;
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="space-y-6">
        <SectionCard eyebrow="Admin" title="Operations panel" description="Manage events, fights, scrape runs, predictions, and moderation.">
          {!token ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Sign in as an admin to use this panel.</div> : null}
          <div className="mb-6 flex flex-wrap gap-4">
            <form onSubmit={triggerPrewarm}>
              <button className="rounded-2xl bg-accent px-4 py-3 text-sm font-semibold text-white">Trigger ML prewarm</button>
            </form>
            <form onSubmit={triggerScraper}>
              <button className="rounded-2xl bg-accent px-4 py-3 text-sm font-semibold text-white">Trigger Scraper</button>
            </form>
          </div>
          <div className="space-y-3">
            {logs.map((log) => (
              <div key={log.id} className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/75">
                <div className="font-semibold text-white">{log.status}</div>
                <div className="mt-1 text-white/55">Events: {log.eventsFound ?? 0} • Fights: {log.fightsUpdated ?? 0}</div>
              </div>
            ))}
            {logs.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">No scraper logs loaded yet.</div> : null}
          </div>
        </SectionCard>

        <SectionCard eyebrow="Users" title="Moderation tools" description="Inspect accounts, adjust roles, and lock or unlock users.">
          {!token ? (
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Sign in as an admin to manage users.</div>
          ) : (
            <div className="space-y-3">
              {users.map((user) => (
                <div key={user.id} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <p className="text-xs uppercase tracking-[0.3em] text-gold">User #{user.id}</p>
                      <h3 className="mt-2 text-lg font-semibold text-white">{user.username ?? "Unknown user"}</h3>
                      <p className="mt-1 text-sm text-white/55">{user.email ?? "No email"}</p>
                      <p className="mt-1 text-xs text-white/45">
                        {user.locked ? "Locked" : "Unlocked"} • {user.enabled ? "Enabled" : "Disabled"}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => void toggleLock(user.id, !user.locked)}
                      disabled={user.role?.name === "ROLE_ADMIN"}
                      className="rounded-2xl border border-white/10 bg-bg/70 px-4 py-2 text-sm text-white/80 transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      {user.locked ? "Unlock" : "Lock"}
                    </button>
                  </div>

                  <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto] md:items-end">
                    <label className="space-y-2 text-sm text-white/70">
                      <span>Role</span>
                      <input
                        value={roleDrafts[user.id] ?? user.role?.name ?? ""}
                        onChange={(event) => handleRoleChange(user.id, event)}
                        disabled={user.role?.name === "ROLE_ADMIN"}
                        className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35 disabled:cursor-not-allowed disabled:opacity-50"
                        placeholder="ROLE_USER"
                      />
                    </label>
                    <button
                      type="button"
                      onClick={() => void saveRole(user.id)}
                      disabled={user.role?.name === "ROLE_ADMIN"}
                      className="rounded-2xl bg-accent px-4 py-3 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Save role
                    </button>
                  </div>
                </div>
              ))}
              {users.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">No users loaded yet.</div> : null}
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  );
}