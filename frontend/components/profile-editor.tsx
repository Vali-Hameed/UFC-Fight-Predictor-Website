"use client";

import { FormEvent, useEffect, useState } from "react";
import { apiFetch, ProfileDto } from "@/lib/api";
import { useAuth } from "@/lib/session";
import { toast } from "sonner";

type ProfileEditorProps = {
  username: string;
};

export function ProfileEditor({ username }: ProfileEditorProps) {
  const { token } = useAuth();
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [profileImageUrl, setProfileImageUrl] = useState("");
  const [publicProfile, setPublicProfile] = useState(true);
  const [optOutEmailNotifications, setOptOutEmailNotifications] = useState(false);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [saving, setSaving] = useState(false);
  const [resetting, setResetting] = useState(false);

  useEffect(() => {
    if (!token) {
      setProfile(null);
      setFirstName("");
      setLastName("");
      setProfileImageUrl("");
      setPublicProfile(true);
      setOptOutEmailNotifications(false);
      return;
    }

    let active = true;
    setLoadingProfile(true);

    void apiFetch<ProfileDto>("/api/v1/users/me", {}, token)
      .then((currentUser) => {
        if (!active) {
          return;
        }

        setProfile(currentUser);
        setFirstName(currentUser.firstName ?? "");
        setLastName(currentUser.lastName ?? "");
        setProfileImageUrl(currentUser.profileImageUrl ?? "");
        setPublicProfile(currentUser.publicProfile ?? true);
        setOptOutEmailNotifications(currentUser.optOutEmailNotifications ?? false);
      })
      .catch(() => {
        if (active) {
          toast.error("Could not load your profile details.");
        }
      })
      .finally(() => {
        if (active) {
          setLoadingProfile(false);
        }
      });

    return () => {
      active = false;
    };
  }, [token]);

  if (!token) {
    return <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Sign in to edit your profile.</div>;
  }

  if (loadingProfile) {
    return <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Loading profile editor...</div>;
  }

  if (!profile) {
    return <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Profile editor unavailable.</div>;
  }

  if (profile.username !== username) {
    return <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">This is a public profile view. Open your own profile to update your details.</div>;
  }

  const saveProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) {
      return;
    }

    setSaving(true);

    try {
      const updatedProfile = await apiFetch<ProfileDto>(
        "/api/v1/users/me",
        {
          method: "PUT",
          body: JSON.stringify({
            firstName,
            lastName,
            profileImageUrl,
            publicProfile,
            optOutEmailNotifications
          })
        },
        token
      );
      setProfile(updatedProfile);
      setFirstName(updatedProfile.firstName ?? "");
      setLastName(updatedProfile.lastName ?? "");
      setProfileImageUrl(updatedProfile.profileImageUrl ?? "");
      setPublicProfile(updatedProfile.publicProfile ?? true);
      setOptOutEmailNotifications(updatedProfile.optOutEmailNotifications ?? false);
      toast.success("Profile updated.");
    } catch {
      toast.error("Could not update your profile.");
    } finally {
      setSaving(false);
    }
  };

  const requestPasswordReset = async () => {
    if (!token) return;
    setResetting(true);
    try {
      await apiFetch("/api/v1/password/request-me", { method: "POST" }, token);
      toast.success("Password reset link sent to your email.");
    } catch {
      toast.error("Failed to send reset link.");
    } finally {
      setResetting(false);
    }
  };

  return (
    <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-gold">Your profile</p>
          <h3 className="mt-2 text-xl font-semibold text-white">Edit account details</h3>
          <p className="mt-2 max-w-2xl text-sm text-white/60">Update the name shown on your public profile.</p>
        </div>
      </div>

      <form onSubmit={saveProfile} className="grid gap-4 md:grid-cols-3">
        <label className="space-y-2 text-sm text-white/70">
          <span>First name</span>
          <input
            name="firstName"
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
            className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35"
            placeholder="First name"
          />
        </label>
        <label className="space-y-2 text-sm text-white/70">
          <span>Last name</span>
          <input
            name="lastName"
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
            className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35"
            placeholder="Last name"
          />
        </label>
        <div className="md:col-span-3">
          <p className="mb-2 text-sm text-white/70">Profile Visibility</p>
          <div className="flex items-center gap-3">
            <span className={`text-sm ${!publicProfile ? 'font-medium text-white' : 'text-white/40'}`}>Private</span>
            <button
              type="button"
              role="switch"
              aria-checked={publicProfile}
              onClick={() => setPublicProfile(!publicProfile)}
              className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus-visible:ring-2 focus-visible:ring-white/75 ${
                publicProfile ? 'bg-accent' : 'bg-white/20'
              }`}
            >
              <span
                className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                  publicProfile ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
            <span className={`text-sm ${publicProfile ? 'font-medium text-white' : 'text-white/40'}`}>Public</span>
          </div>
          <p className="mt-2 text-xs text-white/40">
            Public profiles are visible on the leaderboard and allow others to see your stats and predictions.
          </p>
        </div>
        <div className="md:col-span-3 mt-2">
          <p className="mb-2 text-sm text-white/70">Notification Preferences</p>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={optOutEmailNotifications}
              onChange={(e) => setOptOutEmailNotifications(e.target.checked)}
              className="h-4 w-4 rounded border-white/10 bg-bg/70 accent-accent cursor-pointer"
            />
            <span className="text-sm text-white">Opt-out of all email notifications</span>
          </label>
        </div>
        <div className="md:col-span-3 flex items-center gap-3">
          <button
            type="submit"
            disabled={saving}
            className="rounded-2xl bg-accent px-5 py-3 text-sm font-semibold text-white transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {saving ? "Saving..." : "Save profile"}
          </button>
          <p className="text-xs text-white/45">Changes apply to your account and public profile display.</p>
        </div>
      </form>

      <div className="mt-8 border-t border-white/10 pt-6">
        <h4 className="mb-2 text-lg font-medium text-white">Security</h4>
        <p className="mb-4 text-sm text-white/60">Need to change your password? We will send a reset link to your registered email address.</p>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={requestPasswordReset}
            disabled={resetting}
            className="rounded-2xl border border-white/20 bg-transparent px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {resetting ? "Sending..." : "Reset password"}
          </button>
        </div>
      </div>
    </div>
  );
}
