ALTER TABLE server_notifications
  ADD COLUMN image_path VARCHAR(255) DEFAULT NULL AFTER message,
  ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0 AFTER image_path;
