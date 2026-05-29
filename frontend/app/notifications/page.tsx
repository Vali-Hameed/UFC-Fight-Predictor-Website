import { SectionCard } from "@/components/section-card";
import { NotificationCenter } from "@/components/notification-center";

export default function NotificationsPage() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Inbox" title="Notifications" description="Result notifications, reminders, and leaderboard milestones live here.">
        <NotificationCenter />
      </SectionCard>
    </div>
  );
}