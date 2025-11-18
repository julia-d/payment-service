CREATE TABLE IF NOT EXISTS idempotency_key (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  value TEXT NOT NULL UNIQUE,
  request_hash TEXT NOT NULL,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS payment (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  idempotency_id INTEGER NOT NULL UNIQUE,
  gateway_payment_id TEXT NOT NULL UNIQUE,
  amount_minor INTEGER NOT NULL,
  currency TEXT NOT NULL,
  status TEXT NOT NULL,
  order_id TEXT NOT NULL,
  created_at TEXT NOT NULL,
  metadata TEXT,
  message TEXT,
  FOREIGN KEY (idempotency_id) REFERENCES idempotency_key(id)
);

