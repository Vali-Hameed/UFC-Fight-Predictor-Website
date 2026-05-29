#!/usr/bin/env bash
set -euo pipefail

# Convenience script: create branch, commit all changes, push to origin
# Use this for a single aggregated commit. If you want granular commits,
# follow the manual `git add -p` sequence in COMMIT_HISTORY_SUGGESTED.md

BRANCH=feat/backend-complete
MSG="chore: complete backend — auth, ml, prewarm, results, scraper, password reset, leaderboard, rate-limiting, validation"

echo "Creating branch $BRANCH and committing all changes..."
git fetch origin
git checkout -b "$BRANCH"
git add .
git commit -m "$MSG" || { echo "Nothing to commit"; exit 0; }
git push -u origin "$BRANCH"

echo "Pushed branch $BRANCH. Please open a PR on GitHub to continue code review." 
