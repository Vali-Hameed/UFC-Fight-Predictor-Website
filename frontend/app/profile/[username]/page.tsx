import { SectionCard } from "@/components/section-card";
import { ProfileEditor } from "@/components/profile-editor";
import { apiFetch, ProfileDto } from "@/lib/api";

type ProfilePageProps = {
  params: Promise<{ username: string }>;
};

export default async function ProfilePage({ params }: ProfilePageProps) {
  const { username } = await params;
  const profile = await apiFetch<ProfileDto>(`/api/v1/users/${username}`).catch(() => null);

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Profile" title={`@${username}`} description="Public profiles show rank, points, win rate, and prediction history.">
        {profile ? (
          <div className="space-y-6">
            <div className="grid gap-4 md:grid-cols-3">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <p className="text-sm text-white/50">Username</p>
                <p className="mt-2 text-3xl font-semibold text-white">{profile.username}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <p className="text-sm text-white/50">Role</p>
                <p className="mt-2 text-3xl font-semibold text-white">{profile.role ?? "USER"}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <p className="text-sm text-white/50">Visibility</p>
                <p className="mt-2 text-3xl font-semibold text-white">{profile.enabled ? "Public" : "Private"}</p>
              </div>
            </div>

            <ProfileEditor username={username} />
          </div>
        ) : (
          <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
            Profile data could not be loaded.
          </div>
        )}
      </SectionCard>
    </div>
  );
}