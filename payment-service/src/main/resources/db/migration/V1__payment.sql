CREATE TABLE IF NOT EXISTS payments (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL UNIQUE,
  amount_cents BIGINT NOT NULL CHECK(amount_cents >= 0),
  status VARCHAR(30) NOT NULL,
  provider_reference VARCHAR(200) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE IF NOT EXISTS processed_events(event_id UUID PRIMARY KEY, processed_at TIMESTAMPTZ NOT NULL);
CREATE TABLE IF NOT EXISTS outbox_events(
  id UUID PRIMARY KEY,
  aggregate_id UUID NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  published_at TIMESTAMPTZ NULL
);
CREATE INDEX IF NOT EXISTS idx_payment_outbox_unpublished ON outbox_events(created_at) WHERE published_at IS NULL;
