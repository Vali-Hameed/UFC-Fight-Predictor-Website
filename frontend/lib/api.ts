export const API_BASE_URL = typeof window === 'undefined'
  ? (process.env.INTERNAL_API_URL ?? "http://backend:8080")
  : (process.env.NEXT_PUBLIC_API_URL || "");

export type ApiError = {
  error?: string;
  message?: string;
  details?: Record<string, string>;
};

export class ApiResponseError extends Error {
  status: number;
  errorCode?: string;

  constructor(status: number, message: string, errorCode?: string) {
    super(message);
    this.name = "ApiResponseError";
    this.status = status;
    this.errorCode = errorCode;
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  const text = await response.text();

  if (!response.ok) {
    let errorMsg = "Request failed";
    let errorCode = undefined;
    if (text) {
      try {
        const errData = JSON.parse(text) as ApiError;
        errorMsg = errData.message ?? errorMsg;
        errorCode = errData.error;
      } catch {
        errorMsg = text;
      }
    }
    throw new ApiResponseError(response.status, errorMsg, errorCode);
  }

  if (!text) {
    return undefined as T;
  }

  let data: unknown;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }

  return data as T;
}

export async function apiFetch<T>(path: string, options: RequestInit = {}, token?: string | null): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: "include",
    cache: "no-store"
  });

  if (response.status === 204) {
    return undefined as T;
  }

  return parseResponse<T>(response);
}

export async function markNotificationAsRead(id: number, token: string): Promise<void> {
  await apiFetch(`/api/v1/notifications/${id}/read`, { method: "PATCH" }, token);
}

export async function markAllNotificationsAsRead(token: string): Promise<void> {
  await apiFetch("/api/v1/notifications/read-all", { method: "PATCH" }, token);
}

export async function getUnreadNotificationCount(token: string): Promise<number> {
  return apiFetch<number>(`/api/v1/notifications/unread-count`, {}, token);
}

export async function toggleThreadSubscription(threadId: number, token: string): Promise<boolean> {
  return apiFetch<boolean>(`/api/v1/forum/threads/${threadId}/subscribe`, { method: "POST" }, token);
}

export async function getSubscriptionStatus(threadId: number, token?: string | null): Promise<boolean> {
  return apiFetch<boolean>(`/api/v1/forum/threads/${threadId}/subscription-status`, {}, token);
}

export async function warnUser(userId: number, token: string): Promise<void> {
  await apiFetch(`/api/v1/admin/users/${userId}/warn`, { method: "POST" }, token);
}

export async function banUser(userId: number, token: string, durationDays?: number): Promise<void> {
  const url = `/api/v1/admin/users/${userId}/ban` + (durationDays ? `?durationDays=${durationDays}` : "");
  await apiFetch(url, { method: "POST" }, token);
}

export async function unbanUser(userId: number, token: string): Promise<void> {
  await apiFetch(`/api/v1/admin/users/${userId}/unban`, { method: "POST" }, token);
}

export async function deletePost(postId: number, token: string): Promise<void> {
  await apiFetch(`/api/v1/forum/posts/${postId}/delete`, { method: "PATCH" }, token);
}

export async function deleteUser(userId: number, token: string): Promise<void> {
  await apiFetch(`/api/v1/admin/users/${userId}`, { method: "DELETE" }, token);
}

export async function deleteMyAccount(token: string): Promise<void> {
  await apiFetch(`/api/v1/users/me`, { method: "DELETE" }, token);
}

export async function deleteScrapeLog(logId: number, token: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/v1/internal/scraper/logs/${logId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!res.ok) {
    throw new Error("Failed to delete scrape log");
  }
}

export async function deleteAllScrapeLogs(token: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/v1/internal/scraper/logs`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!res.ok) {
    throw new Error("Failed to delete all scrape logs");
  }
}

export async function deleteScrapeLogsBatch(logIds: number[], token: string): Promise<void> {
  const query = logIds.map((id) => `ids=${id}`).join("&");
  const res = await fetch(`${API_BASE_URL}/api/v1/internal/scraper/logs/batch?${query}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!res.ok) {
    throw new Error("Failed to delete selected scrape logs");
  }
}

export type AuthResponse = {
  accessToken: string;
  expiresInSeconds: number;
};

export type PageResponse<T> = {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
};

export type EventDto = {
  id: number;
  name: string;
  eventDate: string | null;
  location: string | null;
  status: string | null;
  scrapedAt: string | null;
};

export async function getArchivedEvents(page: number, size: number = 20, excludeIds?: number[]): Promise<PageResponse<EventDto>> {
  let url = `/api/v1/events/archived?page=${page}&size=${size}`;
  if (excludeIds && excludeIds.length > 0) {
    url += `&excludeIds=${excludeIds.join(',')}`;
  }
  return apiFetch<PageResponse<EventDto>>(url);
}

export type FightDto = {
  id: number;
  eventId: number;
  fighter1Name: string | null;
  fighter2Name: string | null;
  weightClass: string | null;
  isMainEvent: boolean | null;
  fightOrder: number | null;
  status: string | null;
  resultWinner: string | null;
  resultMethod: string | null;
  resultRound: number | null;
  resultTime: string | null;
};

export type LeaderboardDto = {
  id: number;
  userId: number;
  username: string | null;
  totalPoints: number | null;
  correctPredictions: number | null;
  totalPredictions: number | null;
  currentStreak: number | null;
  bestStreak: number | null;
  lastUpdated: string | null;
};

export async function getEventLeaderboard(eventId: number | string): Promise<LeaderboardDto[]> {
  return apiFetch<LeaderboardDto[]>(`/api/v1/leaderboard/event/${eventId}`);
}

export type NotificationDto = {
  id: number;
  userId: number;
  type: string | null;
  message: string | null;
  read: boolean | null;
  link?: string | null;
  createdAt: string | null;
};

export type ForumThreadDto = {
  id: number;
  eventId: number | null;
  fightId: number | null;
  createdBy: number | null;
  title: string | null;
  createdAt: string | null;
};

export type ForumPostDto = {
  id: number;
  threadId: number | null;
  userId: number | null;
  username?: string | null;
  content: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  isDeleted: boolean | null;
};

export type PredictionHistoryItemDto = {
  fightId: number;
  fighter1Name: string | null;
  fighter2Name: string | null;
  eventId: number | null;
  eventName: string | null;
  predictedWinner: string | null;
  predictedMethod: string | null;
  predictedRound: number | null;
  resultWinner: string | null;
  resultMethod?: string | null;
  resultRound?: number | null;
  submittedAt: string | null;
  locked: boolean;
  pointsAwarded?: number | null;
  isWinnerCorrect?: boolean | null;
};

export type LeaderboardStatsDto = {
  rank: number | null;
  totalPoints: number;
  winRate: number;
};

export type GlobalAccuracyDto = {
  aiAccuracy: number;
  communityAccuracy: number;
  totalAiFights: number;
  totalCommunityPredictions: number;
};

export async function getGlobalAccuracy(): Promise<GlobalAccuracyDto> {
  return apiFetch<GlobalAccuracyDto>("/api/v1/stats/global-accuracy");
}

export type ProfileDto = {
  id: number;
  username: string | null;
  firstName: string | null;
  lastName: string | null;
  profileImageUrl: string | null;
  role: string | null;
  enabled: boolean;
  publicProfile: boolean;
  optOutEmailNotifications?: boolean;
  leaderboardStats?: LeaderboardStatsDto | null;
  predictionHistory?: PredictionHistoryItemDto[] | null;
};

export type UserPredictionRequest = {
  fightId: number;
  predictedWinner: string;
  predictedMethod: string;
  predictedRound: number;
  optOutResultNotification?: boolean;
};

export type MlPredictionDto = {
  id: number;
  fightId: number | null;
  fighter1Name?: string | null;
  fighter2Name?: string | null;
  predictedWinner: string | null;
  confidenceScore: number | null;
  cachedAt: string | null;
};

export type CommunityVoteDto = {
  id: number;
  fightId: number;
  fighter1Votes: number | null;
  fighter2Votes: number | null;
  lastUpdated: string | null;
};

export type ScrapeLogDto = {
  id: number;
  startedAt: string | null;
  completedAt: string | null;
  eventsFound: number | null;
  fightsUpdated: number | null;
  status: string | null;
  errorMessage: string | null;
};

export type AdminRoleDto = {
  id: number | null;
  name: string | null;
};

export type AdminUserDto = {
  id: number;
  firstName: string | null;
  lastName: string | null;
  username: string | null;
  email: string | null;
  profileImageUrl: string | null;
  role: AdminRoleDto | null;
  locked: boolean;
  enabled: boolean;
  bannedFromForumUntil?: string | null;
};

export function getEventDisplayStatus(event: EventDto, mainFightStatus?: string | null): string {
  if (!event.eventDate) return event.status ?? "UNKNOWN";
  const eventTime = new Date(event.eventDate).getTime();
  const now = Date.now();
  const hoursSinceStart = (now - eventTime) / (1000 * 60 * 60);

  if (hoursSinceStart < 0) return "UPCOMING";

  const effectiveStatus = mainFightStatus || event.status;

  if (effectiveStatus === "COMPLETED" || effectiveStatus === "CANCELED") {
    return "COMPLETED";
  }

  if (hoursSinceStart >= 0 && hoursSinceStart <= 12) {
    return "LIVE";
  }

  if (effectiveStatus === "UPCOMING") {
    return "UPCOMING";
  }

  return "COMPLETED";
}