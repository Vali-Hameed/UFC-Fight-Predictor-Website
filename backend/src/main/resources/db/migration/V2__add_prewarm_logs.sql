-- Add prewarm_logs table to persist ML prewarm run metadata
CREATE TABLE prewarm_logs (
  id BIGSERIAL PRIMARY KEY,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  events_found INTEGER DEFAULT 0,
  fights_processed INTEGER DEFAULT 0,
  success_count INTEGER DEFAULT 0,
  failure_count INTEGER DEFAULT 0,
  status VARCHAR(50),
  error_message TEXT
);
