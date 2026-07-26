CREATE TABLE order_proof_images (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES inter_group_orders(id),
    image_url TEXT NOT NULL,
    image_type VARCHAR(20),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    captured_at TIMESTAMP NOT NULL,
    uploaded_by_user_id UUID REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
