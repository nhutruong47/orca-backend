-- ============================================================================
-- Flyway migration: V2__add_chat_attachments.sql
-- Ngày: 26/07/2026 — Quick Win F1.1 (Flyway baseline)
--
-- Lý do:
--   Bổ sung 3 cột attachment cho bảng chat_messages (URL, tên file, loại file).
--   JPA @Transient từ chối expose (vì lý do bảo mật 3rd-party chat app), nên
--   cần persist thẳng vào DB.
--
-- Cách viết:
--   Tách thành 3 ALTER TABLE riêng để tương thích cả H2 (test) và PostgreSQL
--   (prod). PostgreSQL chấp nhận cả 2 cú pháp, H2 chỉ chấp nhận 1 ALTER mỗi
--   ADD COLUMN. Trước đây gộp 1 câu làm H2 fail — đã fix.
--
-- Idempotent: KHÔNG dùng IF NOT EXISTS (PostgreSQL < 9.6 không hỗ trợ). Cần
-- fork một migration `V2.1__safe.sql` nếu chạy lại fail.
-- ============================================================================

ALTER TABLE chat_messages ADD COLUMN attachment_url VARCHAR(255);
ALTER TABLE chat_messages ADD COLUMN attachment_name VARCHAR(255);
ALTER TABLE chat_messages ADD COLUMN attachment_type VARCHAR(100);
