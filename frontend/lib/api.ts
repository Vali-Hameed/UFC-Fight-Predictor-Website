export const API_BASE_URL = typeof window === 'undefined' 
  ? (process.env.INTERNAL_API_URL ?? "http://backend:8080")
  : (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080");

export type ApiError = {
  error?: string;
  message?: string;
  details?: Record<string, string>;
};

async function parseResponse<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  let data: unknown;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }

  if (!response.ok) {
    const error = (data ?? {}) as ApiError;
    throw new Error(error.message ?? "Request failed");
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

export type AuthResponse = {
  accessToken: string;
  expiresInSeconds: number;
};

export type EventDto = {
  id: number;
  name: string;
  eventDate: string | null;
  location: string | null;
  status: string | null;
  scrapedAt: string | null;
};

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
  totalPoints: number | null;
  correctPredictions: number | null;
  totalPredictions: number | null;
  currentStreak: number | null;
  bestStreak: number | null;
  lastUpdated: string | null;
};

export type NotificationDto = {
  id: number;
  userId: number;
  type: string | null;
  message: string | null;
  read: boolean | null;
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
  content: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  isDeleted: boolean | null;
};

export type ProfileDto = {
  id: number;
  username: string | null;
  firstName: string | null;
  lastName: string | null;
  profileImageUrl: string | null;
  role: string | null;
  enabled: boolean;
};

export type UserPredictionRequest = {
  fightId: number;
  predictedWinner: string;
  predictedMethod: string;
  predictedRound: number;
};

export type MlPredictionDto = {
  id: number;
  fightId: number;
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
};