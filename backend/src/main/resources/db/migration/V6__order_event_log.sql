CREATE TABLE order_event_logs (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES inter_group_orders(id),
    actor_user_id UUID REFERENCES users(id),
    actor_role VARCHAR(20),
    event_type VARCHAR(50) NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    note TEXT,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_order_id ON order_event_logs(order_id);
CREATE INDEX idx_created_at ON order_event_logs(created_at);
