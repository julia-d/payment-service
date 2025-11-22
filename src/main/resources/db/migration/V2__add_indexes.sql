CREATE INDEX IF NOT EXISTS idx_payment_status ON payment(status);

CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payment(order_id);

CREATE INDEX IF NOT EXISTS idx_payment_created_at ON payment(created_at);

CREATE INDEX IF NOT EXISTS idx_payment_status_created_at ON payment(status, created_at);

CREATE INDEX IF NOT EXISTS idx_idempotency_key_created_at ON idempotency_key(created_at);

