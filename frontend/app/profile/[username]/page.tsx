export const dynamic = "force-dynamic";

import { SectionCard } from "@/components/section-card";
import { apiFetch, ProfileDto } from "@/lib/api";
import { ProfileView } from "@/components/profile-view";

type ProfilePageProps = {
  params: Promise<{ username: string }>;
};

export default async function ProfilePage({ params }: ProfilePageProps) {
  const { username } = await params;
  const profile = await apiFetch<ProfileDto>(`/api/v1/users/${username}`).catch(() => null);

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Profile" title={`@${username}`} description="Track prediction stats, ranking, and complete pick history.">
        <ProfileView initialProfile={profile} username={username} />
      </SectionCard>
    </div>
  );
}