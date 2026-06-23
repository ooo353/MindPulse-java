-- Migration: Add optimistic lock version column to tasks table
-- Date: 2026-06-23
-- Description: Adds version column for optimistic locking on task status updates

ALTER TABLE `tasks`
ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_at`;

-- Update existing records to have version 0 (already handled by DEFAULT 0)
-- No additional UPDATE needed as DEFAULT 0 handles existing rows

-- Add index for faster version-based queries (optional but recommended)
-- CREATE INDEX idx_task_version ON tasks(id, version);
