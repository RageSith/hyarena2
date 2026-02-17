-- Add recurring season support
ALTER TABLE seasons
  ADD COLUMN recurrence ENUM('none','daily','weekly','monthly','yearly') NOT NULL DEFAULT 'none' AFTER join_code,
  ADD COLUMN recurrence_ends_at DATETIME NULL AFTER recurrence,
  ADD COLUMN base_name VARCHAR(100) NULL AFTER recurrence_ends_at,
  ADD COLUMN iteration INT UNSIGNED NULL AFTER base_name,
  ADD COLUMN parent_season_id INT UNSIGNED NULL AFTER iteration,
  ADD INDEX idx_recurrence (recurrence),
  ADD FOREIGN KEY fk_seasons_parent (parent_season_id) REFERENCES seasons(id) ON DELETE SET NULL;
