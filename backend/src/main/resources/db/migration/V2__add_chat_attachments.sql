-- Thêm cột attachment_url và attachment_name vào bảng chat_messages
ALTER TABLE chat_messages
ADD COLUMN attachment_url VARCHAR(255),
ADD COLUMN attachment_name VARCHAR(255),
ADD COLUMN attachment_type VARCHAR(100);
