CREATE TABLE IF NOT EXISTS inventory_items (
  sku VARCHAR(80) PRIMARY KEY,
  available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
  reserved_quantity INTEGER NOT NULL CHECK (reserved_quantity >= 0),
  version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO inventory_items(sku, available_quantity, reserved_quantity, version) VALUES
('LAPTOP-PRO-14', 25, 0, 0),
('HEADSET-NC-01', 100, 0, 0),
('KEYBOARD-MECH-01', 60, 0, 0)
ON CONFLICT (sku) DO NOTHING;

CREATE TABLE IF NOT EXISTS processed_events (
  event_id UUID PRIMARY KEY,
  processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_events (
  id UUID PRIMARY KEY,
  aggregate_id UUID NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  published_at TIMESTAMPTZ NULL
);
CREATE INDEX IF NOT EXISTS idx_inventory_outbox_unpublished ON outbox_events(created_at) WHERE published_at IS NULL;
