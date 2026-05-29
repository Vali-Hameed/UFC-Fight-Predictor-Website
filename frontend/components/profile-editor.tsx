"use client";

import { FormEvent, useEffect, useState } from "react";
import { apiFetch, ProfileDto } from "@/lib/api";
import { useAuth } from "@/lib/session";

type ProfileEditorProps = {
  username: string;
};

export function ProfileEditor({ username }: ProfileEditorProps) {
  const { token } = useAuth();
  const [profile, setProfile] = useState<ProfileDto | null>(null);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [profileImageUrl, setProfileImageUrl] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!token) {
      setProfile(null);
      setFirstName("");
      setLastName("");
      setProfileImageUrl("");
      setMessage(null);
      return;
    }

    let active = true;
    setLoadingProfile(true);
    setMessage(null);

    void apiFetch<ProfileDto>("/api/v1/users/me", {}, token)
      .then((currentUser) => {
        if (!active) {
          return;
        }

        setProfile(currentUser);
        setFirstName(currentUser.firstName ?? "");
        setLastName(currentUser.lastName ?? "");
        setProfileImageUrl(currentUser.profileImageUrl ?? "");
      })
      .catch(() => {
        if (active) {
          setMessage("Could not load your profile details.");
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
    setMessage(null);

    try {
      const updatedProfile = await apiFetch<ProfileDto>(
        "/api/v1/users/me",
        {
          method: "PUT",
          body: JSON.stringify({
            firstName,
            lastName,
            profileImageUrl
          })
        },
        token
      );
      setProfile(updatedProfile);
      setFirstName(updatedProfile.firstName ?? "");
      setLastName(updatedProfile.lastName ?? "");
      setProfileImageUrl(updatedProfile.profileImageUrl ?? "");
      setMessage("Profile updated.");
    } catch {
      setMessage("Could not update your profile.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-gold">Your profile</p>
          <h3 className="mt-2 text-xl font-semibold text-white">Edit account details</h3>
          <p className="mt-2 max-w-2xl text-sm text-white/60">Update the name and avatar URL shown on your public profile.</p>
        </div>
        {message ? <div className="rounded-full border border-white/10 bg-bg/70 px-4 py-2 text-xs text-white/70">{message}</div> : null}
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
        <label className="space-y-2 text-sm text-white/70">
          <span>Profile image URL</span>
          <input
            name="profileImageUrl"
            value={profileImageUrl}
            onChange={(event) => setProfileImageUrl(event.target.value)}
            className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35"
            placeholder="https://..."
          />
        </label>
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
    </div>
  );
}
