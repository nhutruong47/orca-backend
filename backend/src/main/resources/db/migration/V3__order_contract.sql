CREATE TABLE order_contracts (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES inter_group_orders(id),
    terms TEXT NOT NULL,
    buyer_signature_url TEXT,
    seller_signature_url TEXT,
    signed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'DRAFT',
    file_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
