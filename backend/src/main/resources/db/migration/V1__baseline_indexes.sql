-- ============================================================================
-- ORCA — Production schema baseline.
--
-- Spring Data JPA maintains the development schema automatically
-- (`spring.jpa.hibernate.ddl-auto=update` in dev / H2 in tests), but
-- production deployments run against PostgreSQL where the schema must be
-- authoritative. This file captures the canonical schema + indexes.
--
-- Naming convention: idx_<table>_<cols>.  Multi-column indexes lead with
-- the most selective column.
--
-- Apply with: psql -f V1__baseline.sql
-- ============================================================================

-- ---------- Notifications ----------
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id, is_read);

CREATE INDEX IF NOT EXISTS idx_notifications_created_at
    ON notifications (created_at DESC);

-- ---------- Chat messages ----------
CREATE INDEX IF NOT EXISTS idx_chat_messages_group
    ON chat_messages (group_id, sent_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_messages_dm
    ON chat_messages (direct_message_id, sent_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_messages_sender
    ON chat_messages (sender_id);

-- ---------- Tasks ----------
CREATE INDEX IF NOT EXISTS idx_tasks_assignee_status
    ON tasks (assignee_id, status);

CREATE INDEX IF NOT EXISTS idx_tasks_team_status
    ON tasks (team_id, status);

CREATE INDEX IF NOT EXISTS idx_tasks_due_date
    ON tasks (due_date);

CREATE INDEX IF NOT EXISTS idx_tasks_goal
    ON tasks (goal_id);

CREATE INDEX IF NOT EXISTS idx_tasks_created_at
    ON tasks (created_at DESC);

-- ---------- Production orders ----------
CREATE INDEX IF NOT EXISTS idx_production_orders_status
    ON production_orders (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_production_orders_team
    ON production_orders (team_id);

CREATE INDEX IF NOT EXISTS idx_production_orders_deadline
    ON production_orders (deadline);

-- ---------- Production plans ----------
CREATE INDEX IF NOT EXISTS idx_production_plans_team_active
    ON production_plans (team_id, status);

-- ---------- Inventory ----------
CREATE INDEX IF NOT EXISTS idx_inventory_category
    ON inventory_items (category);

CREATE INDEX IF NOT EXISTS idx_inventory_low_stock
    ON inventory_items (current_stock, reorder_level);

-- ---------- Goals ----------
CREATE INDEX IF NOT EXISTS idx_goals_team_status
    ON goals (team_id, status);

-- ---------- Attendance ----------
CREATE INDEX IF NOT EXISTS idx_attendance_user_date
    ON attendances (user_id, work_date DESC);

-- ---------- Review ----------
CREATE INDEX IF NOT EXISTS idx_reviews_target
    ON reviews (target_user_id);

-- ---------- AI plans ----------
CREATE INDEX IF NOT EXISTS idx_ai_plans_team_status
    ON ai_plans (team_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_plans_owner
    ON ai_plans (owner_id);

-- ---------- Team members ----------
CREATE INDEX IF NOT EXISTS idx_team_members_user
    ON team_members (user_id);

-- ---------- Manufacturing requests ----------
CREATE INDEX IF NOT EXISTS idx_mfg_requests_status
    ON manufacturing_requests (status, created_at DESC);

-- ---------- Inter-group orders ----------
CREATE INDEX IF NOT EXISTS idx_igo_supplier
    ON inter_group_orders (supplier_team_id, status);

CREATE INDEX IF NOT EXISTS idx_igo_receiver
    ON inter_group_orders (receiver_team_id, status);

-- ---------- Cost entries ----------
CREATE INDEX IF NOT EXISTS idx_costs_team_date
    ON costs (team_id, occurred_on DESC);
