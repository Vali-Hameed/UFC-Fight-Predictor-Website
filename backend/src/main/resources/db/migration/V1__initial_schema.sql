-- Initial schema for UFC Fight Predictor
-- Creates core tables for users, roles, tokens, events, fights, predictions, leaderboard, forum, and logs

CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role_id BIGINT REFERENCES roles(id),
  email_verified BOOLEAN DEFAULT FALSE,
  profile_visibility VARCHAR(20) DEFAULT 'PUBLIC',
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE password_reset_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE events (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  event_date TIMESTAMPTZ,
  location VARCHAR(255),
  status VARCHAR(20) DEFAULT 'UPCOMING',
  scraped_at TIMESTAMPTZ
);

CREATE INDEX idx_events_date_status ON events (event_date, status);

CREATE TABLE fights (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
  fighter1_name VARCHAR(255) NOT NULL,
  fighter2_name VARCHAR(255) NOT NULL,
  weight_class VARCHAR(100),
  is_main_event BOOLEAN DEFAULT FALSE,
  fight_order INTEGER,
  status VARCHAR(20) DEFAULT 'UPCOMING',
  result_winner VARCHAR(255),
  result_method VARCHAR(100),
  result_round INTEGER,
  result_time VARCHAR(50)
);

CREATE INDEX idx_fights_event_status ON fights (event_id, status);

CREATE TABLE ml_predictions (
  id BIGSERIAL PRIMARY KEY,
  fight_id BIGINT UNIQUE REFERENCES fights(id) ON DELETE CASCADE,
  predicted_winner VARCHAR(255),
  confidence_score NUMERIC(5,4),
  cached_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE user_predictions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  fight_id BIGINT NOT NULL REFERENCES fights(id) ON DELETE CASCADE,
  predicted_winner VARCHAR(255),
  predicted_method VARCHAR(100),
  predicted_round INTEGER,
  submitted_at TIMESTAMPTZ DEFAULT now(),
  locked BOOLEAN DEFAULT FALSE,
  UNIQUE (user_id, fight_id)
);

CREATE INDEX idx_user_predictions_user ON user_predictions (user_id);
CREATE INDEX idx_user_predictions_fight ON user_predictions (fight_id);

CREATE TABLE prediction_results (
  id BIGSERIAL PRIMARY KEY,
  user_prediction_id BIGINT NOT NULL REFERENCES user_predictions(id) ON DELETE CASCADE,
  is_winner_correct BOOLEAN,
  is_method_correct BOOLEAN,
  is_round_correct BOOLEAN,
  points_awarded INTEGER DEFAULT 0
);

CREATE TABLE leaderboard (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) UNIQUE,
  total_points INTEGER DEFAULT 0,
  correct_predictions INTEGER DEFAULT 0,
  total_predictions INTEGER DEFAULT 0,
  current_streak INTEGER DEFAULT 0,
  best_streak INTEGER DEFAULT 0,
  last_updated TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_leaderboard_points ON leaderboard (total_points DESC);

CREATE TABLE community_votes (
  id BIGSERIAL PRIMARY KEY,
  fight_id BIGINT NOT NULL REFERENCES fights(id) UNIQUE,
  fighter1_votes INTEGER DEFAULT 0,
  fighter2_votes INTEGER DEFAULT 0,
  last_updated TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE forum_threads (
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT REFERENCES events(id),
  fight_id BIGINT REFERENCES fights(id),
  created_by BIGINT REFERENCES users(id),
  title VARCHAR(255),
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE forum_posts (
  id BIGSERIAL PRIMARY KEY,
  thread_id BIGINT NOT NULL REFERENCES forum_threads(id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(id),
  content TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_forum_posts_thread ON forum_posts (thread_id);

CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  type VARCHAR(100),
  message TEXT,
  read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE scrape_logs (
  id BIGSERIAL PRIMARY KEY,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  events_found INTEGER DEFAULT 0,
  fights_updated INTEGER DEFAULT 0,
  status VARCHAR(50),
  error_message TEXT
);
