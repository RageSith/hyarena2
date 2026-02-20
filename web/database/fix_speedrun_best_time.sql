-- Fix speedrun best_time_ms data poisoned by timeout matches (finish_time_nanos=0, is_dnf=false)
-- Run this ONCE after deploying the PHP + Java fixes.

-- Step 1: Fix match_participants.finish_time_ms
-- Recompute from json_data for all speed_run matches.
-- Valid finish = is_dnf is false AND finish_time_nanos > 0.
-- DNF or timeout = set to NULL.
UPDATE match_participants mp
JOIN matches m ON mp.match_id = m.id
SET mp.finish_time_ms = CASE
    WHEN m.game_mode = 'speed_run'
         AND mp.json_data IS NOT NULL
         AND JSON_EXTRACT(mp.json_data, '$.is_dnf') = false
         AND CAST(JSON_EXTRACT(mp.json_data, '$.finish_time_nanos') AS UNSIGNED) > 0
    THEN ROUND(CAST(JSON_EXTRACT(mp.json_data, '$.finish_time_nanos') AS UNSIGNED) / 1000000)
    ELSE NULL
END
WHERE m.game_mode = 'speed_run';

-- Step 2: Recompute player_stats.best_time_ms from corrected match_participants
UPDATE player_stats ps
JOIN arenas a ON ps.arena_id = a.id
LEFT JOIN (
    SELECT mp.player_uuid, m.arena_id, MIN(mp.finish_time_ms) AS best_time
    FROM match_participants mp
    JOIN matches m ON mp.match_id = m.id
    WHERE m.game_mode = 'speed_run'
      AND mp.player_uuid IS NOT NULL
      AND mp.finish_time_ms IS NOT NULL
    GROUP BY mp.player_uuid, m.arena_id
) computed ON ps.player_uuid = computed.player_uuid AND ps.arena_id = computed.arena_id
SET ps.best_time_ms = computed.best_time
WHERE a.game_mode = 'speed_run';

-- Step 3: Recompute season_player_stats.best_time_ms from corrected match_participants
UPDATE season_player_stats sps
LEFT JOIN (
    SELECT mp.player_uuid, sm.season_id, MIN(mp.finish_time_ms) AS best_time
    FROM match_participants mp
    JOIN season_matches sm ON mp.match_id = sm.match_id
    JOIN matches m ON mp.match_id = m.id
    WHERE m.game_mode = 'speed_run'
      AND mp.player_uuid IS NOT NULL
      AND mp.finish_time_ms IS NOT NULL
    GROUP BY mp.player_uuid, sm.season_id
) computed ON sps.player_uuid = computed.player_uuid AND sps.season_id = computed.season_id
SET sps.best_time_ms = computed.best_time
WHERE sps.best_time_ms IS NOT NULL OR computed.best_time IS NOT NULL;
