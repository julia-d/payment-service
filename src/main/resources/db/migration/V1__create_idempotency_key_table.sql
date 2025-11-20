CREATE TABLE IF NOT EXISTS idempotency_key (
                                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                                               key_value TEXT NOT NULL UNIQUE,
                                               request_hash TEXT NOT NULL,
                                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       idempotency_id INTEGER NOT NULL UNIQUE,
                                       gateway_payment_id TEXT UNIQUE,
                                       amount_minor INTEGER NOT NULL,
                                       currency TEXT NOT NULL,
                                       status TEXT NOT NULL,
                                       order_id TEXT NOT NULL,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       metadata TEXT,
                                       message TEXT,
                                       FOREIGN KEY (idempotency_id) REFERENCES idempotency_key(id)
    );
