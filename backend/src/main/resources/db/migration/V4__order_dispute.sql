CREATE TABLE order_disputes (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES inter_group_orders(id),
    opened_by_user_id UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    evidence_urls TEXT,
    compensation_amount DECIMAL(15,2),
    status VARCHAR(20) DEFAULT 'OPEN',
    resolution_note TEXT,
    resolved_by_user_id UUID REFERENCES users(id),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
