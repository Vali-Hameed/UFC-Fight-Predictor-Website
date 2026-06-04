"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/lib/session";
import { getSubscriptionStatus, toggleThreadSubscription } from "@/lib/api";
import { toast } from "sonner";

type SubscribeButtonProps = {
  threadId: number;
};

export function SubscribeButton({ threadId }: SubscribeButtonProps) {
  const { token, user } = useAuth();
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState(false);

  useEffect(() => {
    if (token) {
      getSubscriptionStatus(threadId, token)
        .then((status) => setIsSubscribed(status))
        .catch(() => {})
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [threadId, token]);

  if (!token || loading) return null;

  const handleToggle = async () => {
    setToggling(true);
    try {
      const newStatus = await toggleThreadSubscription(threadId, token);
      setIsSubscribed(newStatus);
      if (newStatus) {
        toast.success("Subscribed to thread notifications.");
      } else {
        toast.success("Unsubscribed from thread notifications.");
      }
    } catch {
      toast.error("Failed to update subscription status.");
    } finally {
      setToggling(false);
    }
  };

  return (
    <button
      onClick={handleToggle}
      disabled={toggling}
      className={`rounded-xl border px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60 ${
        isSubscribed 
          ? "border-white/20 bg-white/10 text-white hover:bg-white/20" 
          : "border-accent/30 bg-accent/20 text-accent hover:bg-accent/40"
      }`}
    >
      {toggling ? "Updating..." : isSubscribed ? "Unsubscribe" : "Subscribe"}
    </button>
  );
}
