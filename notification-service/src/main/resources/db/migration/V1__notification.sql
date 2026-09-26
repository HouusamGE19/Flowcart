CREATE TABLE IF NOT EXISTS notifications(
  id UUID PRIMARY KEY,
  source_event_id UUID NOT NULL UNIQUE,
  order_id UUID NOT NULL,
  channel VARCHAR(30) NOT NULL,
  destination VARCHAR(200) NOT NULL,
  template VARCHAR(80) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
