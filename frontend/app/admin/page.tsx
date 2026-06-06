"use client";

import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { AdminUserDto, apiFetch, ScrapeLogDto, warnUser, banUser, unbanUser, deleteUser, deleteScrapeLog } from "@/lib/api";
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
  // eslint-disable-next-line react-hooks/exhaustive-deps
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

  const handleWarn = async (userId: number) => {
    if (!token) return;
    if (confirm("Warn this user?")) {
      try {
        await warnUser(userId, token);
        toast.success("User warned.");
      } // eslint-disable-next-line @typescript-eslint/no-explicit-any
    catch (error: any) {
        toast.error(error.message || "Failed to warn user.");
      }
    }
  };

  const handleBan = async (userId: number, durationDays?: number) => {
    if (!token) return;
    const msg = durationDays ? `Ban user for ${durationDays} days?` : "Permanently ban this user?";
    if (confirm(msg)) {
      try {
        await banUser(userId, token, durationDays);
        toast.success("User banned.");
      } // eslint-disable-next-line @typescript-eslint/no-explicit-any
    catch (error: any) {
        toast.error(error.message || "Failed to ban user.");
      }
    }
  };

  const handleUnban = async (userId: number) => {
    if (!token) return;
    if (confirm("Remove this user's forum ban?")) {
      try {
        await unbanUser(userId, token);
        toast.success("User unbanned.");
      } // eslint-disable-next-line @typescript-eslint/no-explicit-any
    catch (error: any) {
        toast.error(error.message || "Failed to unban user.");
      }
    }
  };

  const handleDelete = async (userId: number) => {
    if (!token) return;
    if (confirm("Permanently delete this user? This cannot be undone.")) {
      try {
        await deleteUser(userId, token);
        setUsers((current) => current.filter((u) => u.id !== userId));
        toast.success("User deleted.");
      } // eslint-disable-next-line @typescript-eslint/no-explicit-any
    catch (error: any) {
        toast.error(error.message || "Failed to delete user.");
      }
    }
  };

  const handleDeleteLog = async (logId: number) => {
    if (!token) return;
    if (confirm("Delete this scraper log?")) {
      try {
        await deleteScrapeLog(logId, token);
        setLogs((current) => current.filter((l) => l.id !== logId));
        toast.success("Scraper log deleted.");
      } // eslint-disable-next-line @typescript-eslint/no-explicit-any
    catch (error: any) {
        toast.error(error.message || "Failed to delete log.");
      }
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
              <div key={log.id} className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/75 flex justify-between items-center">
                <div>
                  <div className="font-semibold text-white">{log.status}</div>
                  {log.startedAt && <div className="text-xs text-white/50 mt-0.5">Started: {new Date(log.startedAt).toLocaleString()}</div>}
                  {log.completedAt && <div className="text-xs text-white/50">Completed: {new Date(log.completedAt).toLocaleString()}</div>}
                  <div className="mt-1 text-white/55">Events: {log.eventsFound ?? 0} • Fights: {log.fightsUpdated ?? 0}</div>
                </div>
                <button
                  type="button"
                  onClick={() => handleDeleteLog(log.id)}
                  className="rounded bg-red-500/20 px-3 py-1.5 text-xs text-red-400 hover:bg-red-500/40 transition"
                >
                  Delete
                </button>
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

                  <div className="mt-4 flex flex-wrap gap-2 pt-4 border-t border-white/5">
                    <button
                      type="button"
                      onClick={() => handleWarn(user.id)}
                      disabled={user.role?.name === "ROLE_ADMIN"}
                      className="rounded bg-yellow-500/20 px-3 py-1.5 text-xs text-yellow-400 hover:bg-yellow-500/40 transition disabled:opacity-30 disabled:cursor-not-allowed"
                    >
                      Warn
                    </button>
                    <button
                      type="button"
                      onClick={() => handleBan(user.id, 7)}
                      disabled={user.role?.name === "ROLE_ADMIN"}
                      className="rounded bg-orange-500/20 px-3 py-1.5 text-xs text-orange-400 hover:bg-orange-500/40 transition disabled:opacity-30 disabled:cursor-not-allowed"
                    >
                      Ban 7d
                    </button>
                    <button
                      type="button"
                      onClick={() => handleBan(user.id)}
                      disabled={user.role?.name === "ROLE_ADMIN"}
                      className="rounded bg-red-900/40 px-3 py-1.5 text-xs text-red-500 hover:bg-red-900/60 transition disabled:opacity-30 disabled:cursor-not-allowed"
                    >
                      Ban Perm
                    </button>
                    <button
                      type="button"
                      onClick={() => handleUnban(user.id)}
                      disabled={user.role?.name === "ROLE_ADMIN"}
                      className="rounded bg-green-500/20 px-3 py-1.5 text-xs text-green-400 hover:bg-green-500/40 transition disabled:opacity-30 disabled:cursor-not-allowed"
                    >
                      Unban
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDelete(user.id)}
                      disabled={user.role?.name === "ROLE_ADMIN"}
                      className="rounded bg-red-500/20 px-3 py-1.5 text-xs text-red-400 hover:bg-red-500/40 transition disabled:opacity-30 disabled:cursor-not-allowed ml-auto"
                    >
                      Delete User
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