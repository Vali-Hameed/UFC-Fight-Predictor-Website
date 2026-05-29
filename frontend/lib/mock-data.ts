export const upcomingEvents = [
  {
    id: 1,
    name: "UFC 312: Apex Clash",
    date: "2026-06-08T02:00:00Z",
    location: "Las Vegas, NV",
    status: "UPCOMING"
  },
  {
    id: 2,
    name: "Fight Night: Calgary",
    date: "2026-06-15T01:00:00Z",
    location: "Calgary, Alberta",
    status: "LIVE"
  }
];

export const sampleFights = [
  {
    id: 11,
    fighter1Name: "Alex Pereira",
    fighter2Name: "Jamahal Hill",
    weightClass: "Light Heavyweight",
    isMainEvent: true,
    fightOrder: 1,
    status: "UPCOMING",
    mlWinner: "Alex Pereira",
    confidence: 0.71,
    communitySplit: 0.58
  },
  {
    id: 12,
    fighter1Name: "Tom Aspinall",
    fighter2Name: "Ciryl Gane",
    weightClass: "Heavyweight",
    isMainEvent: false,
    fightOrder: 2,
    status: "UPCOMING",
    mlWinner: "Tom Aspinall",
    confidence: 0.64,
    communitySplit: 0.62
  }
];

export const leaderboard = [
  { rank: 1, username: "ironjaw", points: 428, correct: 39, winRate: 82, streak: 6 },
  { rank: 2, username: "octagonpro", points: 401, correct: 36, winRate: 79, streak: 4 },
  { rank: 3, username: "maincard", points: 355, correct: 31, winRate: 74, streak: 3 }
];