# Suggested Commit History

Date: 2026-05-29
Branch: feat/backend-complete (recommended)

Purpose: a Conventional Commits sequence summarizing the backend work completed.

Suggested commits (apply with `git add -p` to split changes interactively):

1) feat(auth): implement JWT auth and refresh-token rotation
   - DB-backed refresh tokens, rotation, secure HttpOnly refresh cookie
   - `AuthController`, `JwtService`, `RefreshTokenService`, security config

2) feat(rate-limit): add IP-based rate limiting and validation
   - `RateLimitingFilter` (Bucket4j + Caffeine)
   - input sanitization utilities and validation annotations

3) feat(ml): integrate ML service with caching and admin refresh
   - `MlService`, `MlController`, caching to `ml_predictions` table
   - admin refresh endpoint

4) feat(prewarm): scheduled ML prewarm and prewarm administration
   - `MlPrewarmService`, `PrewarmLog` entity and Flyway migration (V2)
   - admin toggle + manual trigger endpoints

5) feat(results): result processing and leaderboard updates
   - `ResultProcessingService`, admin result entry, leaderboard update logic

6) feat(scraper): internal scraper endpoints with X-Scraper-Key auth
   - `ScraperController`, `ScraperAuthFilter`

7) feat(password): password reset flow
   - `PasswordResetService`, `PasswordResetController`, token persistence

8) feat(leaderboard): leaderboard read endpoints
   - `LeaderboardController` paginated global and single-user row endpoints

9) chore(migrations): add Flyway V2 for prewarm logs

10) docs(commit-history): add this suggested commit history and git script

Notes:
- If you prefer a single aggregated commit, use the provided script in `git_commit_suggested.sh`.
- For best history, stage related files per commit using `git add -p` and commit each message above.

Example manual sequence:

```bash
# create branch
git checkout -b feat/backend-complete

# stage interactively per logical change and commit
git add -p
git commit -m "feat(auth): implement JWT auth and refresh-token rotation"
# repeat git add -p / git commit for other commits

# push branch
git push -u origin feat/backend-complete
```
