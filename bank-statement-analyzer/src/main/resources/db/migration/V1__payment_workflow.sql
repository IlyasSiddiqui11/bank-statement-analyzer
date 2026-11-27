CREATE TABLE upload_sessions (
    id UUID PRIMARY KEY,
    public_token_hash VARCHAR(64) NOT NULL UNIQUE,
    transaction_count INTEGER NOT NULL CHECK (transaction_count > 0),
    price_per_transaction_paise INTEGER NOT NULL CHECK (price_per_transaction_paise = 25),
    total_amount_paise BIGINT NOT NULL CHECK (total_amount_paise > 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    workbook BYTEA,
    redeemed_at TIMESTAMPTZ
);

CREATE INDEX idx_upload_sessions_expires_at ON upload_sessions(expires_at);
CREATE INDEX idx_upload_sessions_status ON upload_sessions(status);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    upload_session_id UUID NOT NULL REFERENCES upload_sessions(id),
    razorpay_order_id VARCHAR(64) NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(64),
    amount_paise BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    signature_verified BOOLEAN NOT NULL DEFAULT FALSE,
    captured BOOLEAN NOT NULL DEFAULT FALSE,
    failure_message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_payments_razorpay_payment_id
    ON payments(razorpay_payment_id) WHERE razorpay_payment_id IS NOT NULL;
CREATE INDEX idx_payments_session ON payments(upload_session_id);

CREATE TABLE download_records (
    id UUID PRIMARY KEY,
    upload_session_id UUID NOT NULL REFERENCES upload_sessions(id),
    payment_id UUID NOT NULL REFERENCES payments(id),
    download_status VARCHAR(32) NOT NULL,
    redemption_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    redeemed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_download_records_session ON download_records(upload_session_id);

CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    razorpay_event_id VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(128) NOT NULL,
    processing_status VARCHAR(32) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ
);
