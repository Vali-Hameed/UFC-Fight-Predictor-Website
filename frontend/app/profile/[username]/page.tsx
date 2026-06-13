export const dynamic = "force-dynamic";

import type { Metadata } from "next";
import { SectionCard } from "@/components/section-card";

export async function generateMetadata({ params }: { params: Promise<{ username: string }> }): Promise<Metadata> {
  const { username } = await params;
  try {
    const profile = await apiFetch<ProfileDto>(`/api/v1/users/${username}`);
    if (profile) {
      return {
        title: `@${profile.username} Profile | FightPicks`,
        description: `View @${profile.username}'s fight prediction stats, ranking, and history on FightPicks.`,
        openGraph: {
          title: `@${profile.username} | FightPicks`,
          description: `View @${profile.username}'s fight prediction stats and ranking.`,
        },
      };
    }
  } catch (err) {
    // Fallback
  }
  return {
    title: `@${username} Profile | FightPicks`,
    description: `View @${username}'s UFC fight prediction stats and history.`,
  };
}
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