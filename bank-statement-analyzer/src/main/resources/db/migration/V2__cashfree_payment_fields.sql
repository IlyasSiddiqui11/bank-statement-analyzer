ALTER TABLE payments
    ALTER COLUMN razorpay_order_id DROP NOT NULL;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS cashfree_order_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS cashfree_payment_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS payment_session_id VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_cashfree_order_id
    ON payments(cashfree_order_id) WHERE cashfree_order_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_cashfree_payment_id
    ON payments(cashfree_payment_id) WHERE cashfree_payment_id IS NOT NULL;

ALTER TABLE webhook_events
    ALTER COLUMN razorpay_event_id DROP NOT NULL;

ALTER TABLE webhook_events
    ADD COLUMN IF NOT EXISTS cashfree_event_id VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uq_webhook_events_cashfree_event_id
    ON webhook_events(cashfree_event_id) WHERE cashfree_event_id IS NOT NULL;
