"use client";

import Link from "next/link";
import type { BadgeDto } from "@/lib/api";

type CosmeticUsernameProps = {
  username: string;
  cosmeticGlowColor?: string | null;
  cosmeticTitle?: string | null;
  badges?: BadgeDto[] | null;
  size?: "sm" | "md" | "lg";
  linkToProfile?: boolean;
  showBadges?: boolean;
  showTitle?: boolean;
};

const BADGE_ICONS: Record<string, { emoji: string; color: string }> = {
  SEASON_CHAMPION: { emoji: "👑", color: "#FFD700" },
  SEASON_SILVER: { emoji: "🥈", color: "#C0C0C0" },
  SEASON_BRONZE: { emoji: "🥉", color: "#CD7F32" },
  EVENT_WINNER: { emoji: "🏆", color: "#E53E3E" },
  PERFECT_EVENT: { emoji: "💎", color: "#00BFFF" },
  STREAK_10: { emoji: "🔥", color: "#FF6B35" },
  STREAK_25: { emoji: "⚡", color: "#FF4500" },
};

function getTitleColor(title: string) {
  if (title.includes("Champion")) return "#c9a84c"; // Gold
  if (title.includes("Silver")) return "#c0c0c0"; // Silver
  if (title.includes("Bronze")) return "#cd7f32"; // Bronze
  if (title.includes("Event Winner")) return "#ef4444"; // Red
  return "#888888"; // Default
}

export function CosmeticUsername({
  username,
  cosmeticGlowColor,
  cosmeticTitle,
  badges,
  size = "md",
  linkToProfile = true,
  showBadges = false,
  showTitle = false,
}: CosmeticUsernameProps) {
  const hasGlow = !!cosmeticGlowColor;
  const hasSeasonChampion = badges?.some((b) => b.badgeType === "SEASON_CHAMPION");
  const hasSeasonSilver = badges?.some((b) => b.badgeType === "SEASON_SILVER");
  const hasSeasonBronze = badges?.some((b) => b.badgeType === "SEASON_BRONZE");

  const sizeClasses = {
    sm: "text-xs",
    md: "text-sm",
    lg: "text-base",
  };

  // Build inline style for the username text
  const nameStyle: React.CSSProperties = {};
  if (hasGlow && cosmeticGlowColor) {
    if (cosmeticGlowColor.startsWith("linear-gradient")) {
      nameStyle.background = cosmeticGlowColor;
      nameStyle.WebkitBackgroundClip = "text";
      nameStyle.WebkitTextFillColor = "transparent";
      nameStyle.backgroundClip = "text";
    } else {
      nameStyle.color = cosmeticGlowColor;
      nameStyle.textShadow = `0 0 8px ${cosmeticGlowColor}60, 0 0 20px ${cosmeticGlowColor}30`;
    }
  }

  // Determine unique badge types to display (deduplicate, but show EVENT_WINNER count)
  const uniqueBadgeTypes = new Map<string, { count: number; badge: BadgeDto }>();
  if (badges) {
    for (const badge of badges) {
      const existing = uniqueBadgeTypes.get(badge.badgeType);
      if (existing) {
        existing.count++;
      } else {
        uniqueBadgeTypes.set(badge.badgeType, { count: 1, badge });
      }
    }
  }

  let animationClass = "";
  if (hasSeasonChampion) animationClass = "animate-champion-glow";
  else if (hasSeasonSilver) animationClass = "animate-silver-glow";
  else if (hasSeasonBronze) animationClass = "animate-bronze-glow";

  const nameContent = (
    <span
      className={`font-semibold ${sizeClasses[size]} ${
        hasGlow ? "" : "text-white"
      } ${animationClass}`}
      style={nameStyle}
    >
      @{username}
    </span>
  );

  return (
    <span className="inline-flex items-center gap-1.5 flex-wrap">
      {/* Badge icons before name */}
      {showBadges &&
        Array.from(uniqueBadgeTypes.entries()).map(([type, { count, badge }]) => {
          const config = BADGE_ICONS[type] || { emoji: "🎖️", color: "#888" };
          return (
            <span
              key={type}
              title={count > 1 ? `${count}x ${badge.badgeLabel}` : badge.badgeLabel}
              className="inline-flex items-center"
              style={{ filter: `drop-shadow(0 0 4px ${config.color}60)` }}
            >
              <span className="text-xs">{config.emoji}</span>
              {type === "EVENT_WINNER" && count > 1 && (
                <span
                  className="text-[10px] font-bold ml-0.5"
                  style={{ color: config.color }}
                >
                  {count}x
                </span>
              )}
            </span>
          );
        })}

      {/* Username (optionally linked) */}
      {linkToProfile ? (
        <Link href={`/profile/${username}`} className="hover:underline">
          {nameContent}
        </Link>
      ) : (
        nameContent
      )}

      {/* Cosmetic title */}
      {showTitle && cosmeticTitle && (
        <span
          className="text-[10px] uppercase tracking-widest font-medium px-1.5 py-0.5 rounded-full border"
          style={{
            borderColor: `${getTitleColor(cosmeticTitle)}40`,
            color: getTitleColor(cosmeticTitle),
            backgroundColor: `${getTitleColor(cosmeticTitle)}10`,
          }}
        >
          {cosmeticTitle}
        </span>
      )}
    </span>
  );
}
