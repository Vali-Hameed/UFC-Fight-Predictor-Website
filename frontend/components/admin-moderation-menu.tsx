"use client";

import { useAuth } from "@/lib/session";
import { warnUser, banUser, deletePost } from "@/lib/api";
import { toast } from "sonner";
import { useRouter } from "next/navigation";

type AdminModerationMenuProps = {
  postId: number;
  postUserId: number;
};

export function AdminModerationMenu({ postId, postUserId }: AdminModerationMenuProps) {
  const { token, user } = useAuth();
  const router = useRouter();

  if (!token || user?.role !== "ROLE_ADMIN") {
    // Post authors can also delete their own posts, we check this logic here:
    if (user?.id === postUserId) {
        return (
            <button 
                onClick={async () => {
                    if (confirm("Are you sure you want to delete this post?")) {
                        try {
                            await deletePost(postId, token!);
                            toast.success("Post deleted.");
                            router.refresh();
                        } catch {
                            toast.error("Failed to delete post.");
                        }
                    }
                }}
                className="mt-2 text-xs font-semibold text-red-500 hover:text-red-400 transition"
            >
                Delete
            </button>
        );
    }
    return null;
  }

  const handleWarn = async () => {
    if (confirm("Warn this user?")) {
      try {
        await warnUser(postUserId, token);
        toast.success("User warned.");
      } catch (error: any) {
        toast.error(error.message || "Failed to warn user.");
      }
    }
  };

  const handleBan = async (durationDays?: number) => {
    const msg = durationDays ? `Ban user for ${durationDays} days?` : "Permanently ban this user?";
    if (confirm(msg)) {
      try {
        await banUser(postUserId, token, durationDays);
        toast.success("User banned.");
      } catch (error: any) {
        toast.error(error.message || "Failed to ban user.");
      }
    }
  };

  const handleDelete = async () => {
    if (confirm("Delete this post?")) {
      try {
        await deletePost(postId, token);
        toast.success("Post deleted.");
        router.refresh();
      } catch (error: any) {
        toast.error(error.message || "Failed to delete post.");
      }
    }
  };

  const isSelf = user?.id === postUserId;

  return (
    <div className="mt-4 flex flex-wrap gap-2 pt-2 border-t border-white/5">
      <span className="text-[10px] uppercase tracking-widest text-white/30 mr-2 flex items-center">Admin</span>
      <button onClick={handleDelete} className="rounded bg-red-500/20 px-2 py-1 text-xs text-red-400 hover:bg-red-500/40 transition">
        Delete Post
      </button>
      {!isSelf && (
        <>
          <button onClick={handleWarn} className="rounded bg-yellow-500/20 px-2 py-1 text-xs text-yellow-400 hover:bg-yellow-500/40 transition">
            Warn User
          </button>
          <button onClick={() => handleBan(7)} className="rounded bg-orange-500/20 px-2 py-1 text-xs text-orange-400 hover:bg-orange-500/40 transition">
            Ban 7d
          </button>
          <button onClick={() => handleBan()} className="rounded bg-red-900/40 px-2 py-1 text-xs text-red-500 hover:bg-red-900/60 transition">
            Ban Perm
          </button>
        </>
      )}
    </div>
  );
}
