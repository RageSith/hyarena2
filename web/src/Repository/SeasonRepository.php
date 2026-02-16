<?php

namespace App\Repository;

use App\Database;
use PDO;

class SeasonRepository
{
    // ==========================================
    // CRUD
    // ==========================================

    public function create(array $data): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO seasons
                (name, slug, description, type, status, starts_at, ends_at,
                 ranking_mode, ranking_config, min_matches,
                 arena_ids, game_mode_ids, visibility, join_code)
            VALUES
                (:name, :slug, :description, :type, :status, :starts_at, :ends_at,
                 :ranking_mode, :ranking_config, :min_matches,
                 :arena_ids, :game_mode_ids, :visibility, :join_code)
        ');
        $stmt->execute([
            'name' => $data['name'],
            'slug' => $data['slug'],
            'description' => $data['description'] ?? null,
            'type' => $data['type'] ?? 'system',
            'status' => $data['status'] ?? 'draft',
            'starts_at' => $data['starts_at'],
            'ends_at' => $data['ends_at'],
            'ranking_mode' => $data['ranking_mode'] ?? 'wins',
            'ranking_config' => isset($data['ranking_config']) ? json_encode($data['ranking_config']) : null,
            'min_matches' => $data['min_matches'] ?? 5,
            'arena_ids' => isset($data['arena_ids']) ? json_encode($data['arena_ids']) : null,
            'game_mode_ids' => isset($data['game_mode_ids']) ? json_encode($data['game_mode_ids']) : null,
            'visibility' => $data['visibility'] ?? 'public',
            'join_code' => $data['join_code'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public function update(int $id, array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            UPDATE seasons SET
                name = :name,
                slug = :slug,
                description = :description,
                type = :type,
                starts_at = :starts_at,
                ends_at = :ends_at,
                ranking_mode = :ranking_mode,
                ranking_config = :ranking_config,
                min_matches = :min_matches,
                arena_ids = :arena_ids,
                game_mode_ids = :game_mode_ids,
                visibility = :visibility,
                join_code = :join_code
            WHERE id = :id
        ');
        $stmt->execute([
            'id' => $id,
            'name' => $data['name'],
            'slug' => $data['slug'],
            'description' => $data['description'] ?? null,
            'type' => $data['type'] ?? 'system',
            'starts_at' => $data['starts_at'],
            'ends_at' => $data['ends_at'],
            'ranking_mode' => $data['ranking_mode'] ?? 'wins',
            'ranking_config' => isset($data['ranking_config']) ? json_encode($data['ranking_config']) : null,
            'min_matches' => $data['min_matches'] ?? 5,
            'arena_ids' => isset($data['arena_ids']) ? json_encode($data['arena_ids']) : null,
            'game_mode_ids' => isset($data['game_mode_ids']) ? json_encode($data['game_mode_ids']) : null,
            'visibility' => $data['visibility'] ?? 'public',
            'join_code' => $data['join_code'] ?? null,
        ]);
    }

    public function findById(int $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM seasons WHERE id = :id');
        $stmt->execute(['id' => $id]);
        return $stmt->fetch() ?: null;
    }

    public function findBySlug(string $slug): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM seasons WHERE slug = :slug');
        $stmt->execute(['slug' => $slug]);
        return $stmt->fetch() ?: null;
    }

    public function findByJoinCode(string $code): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM seasons WHERE join_code = :code AND status = :status');
        $stmt->execute(['code' => $code, 'status' => 'active']);
        return $stmt->fetch() ?: null;
    }

    public function updateStatus(int $id, string $status): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE seasons SET status = :status WHERE id = :id');
        $stmt->execute(['id' => $id, 'status' => $status]);
    }

    public function delete(int $id): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM seasons WHERE id = :id AND status = :status');
        $stmt->execute(['id' => $id, 'status' => 'draft']);
    }

    public function getAll(): array
    {
        $db = Database::getConnection();
        $stmt = $db->query('
            SELECT s.*,
                   (SELECT COUNT(*) FROM season_participants sp WHERE sp.season_id = s.id) AS participant_count
            FROM seasons s
            ORDER BY s.created_at DESC
        ');
        return $stmt->fetchAll();
    }

    // ==========================================
    // Public Queries
    // ==========================================

    public function getPublicActive(): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT s.*,
                   (SELECT COUNT(*) FROM season_participants sp WHERE sp.season_id = s.id) AS participant_count
            FROM seasons s
            WHERE s.status = :status AND s.visibility = :visibility
            ORDER BY s.ends_at ASC
        ');
        $stmt->execute(['status' => 'active', 'visibility' => 'public']);
        return $stmt->fetchAll();
    }

    public function getPublicEnded(int $limit = 25, int $offset = 0): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT s.*,
                   (SELECT COUNT(*) FROM season_participants sp WHERE sp.season_id = s.id) AS participant_count
            FROM seasons s
            WHERE s.status IN (:ended, :archived) AND s.visibility = :visibility
            ORDER BY s.ends_at DESC
            LIMIT :limit OFFSET :offset
        ');
        $stmt->bindValue('ended', 'ended');
        $stmt->bindValue('archived', 'archived');
        $stmt->bindValue('visibility', 'public');
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function getPublicEndedCount(): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT COUNT(*) FROM seasons
            WHERE status IN (:ended, :archived) AND visibility = :visibility
        ');
        $stmt->execute(['ended' => 'ended', 'archived' => 'archived', 'visibility' => 'public']);
        return (int) $stmt->fetchColumn();
    }

    // ==========================================
    // Match Integration
    // ==========================================

    public function getActiveSeasonsForMatch(string $arenaId, string $gameMode): array
    {
        $logFile = __DIR__ . '/../../logs/season_debug.log';
        @mkdir(dirname($logFile), 0775, true);

        $db = Database::getConnection();
        $now = date('Y-m-d H:i:s');

        file_put_contents($logFile, date('[Y-m-d H:i:s] ') . "DB query: status=active, now=$now, arena=$arenaId, mode=$gameMode\n", FILE_APPEND);

        $stmt = $db->prepare('
            SELECT * FROM seasons
            WHERE status = :status
              AND starts_at <= :now_start
              AND ends_at >= :now_end
        ');
        $stmt->execute(['status' => 'active', 'now_start' => $now, 'now_end' => $now]);
        $seasons = $stmt->fetchAll();

        file_put_contents($logFile, date('[Y-m-d H:i:s] ') . "SQL returned " . count($seasons) . " season(s)" . ($seasons ? ': ' . json_encode(array_map(fn($s) => ['id' => $s['id'], 'name' => $s['name'], 'starts_at' => $s['starts_at'], 'ends_at' => $s['ends_at'], 'arena_ids' => $s['arena_ids'], 'game_mode_ids' => $s['game_mode_ids']], $seasons)) : '') . "\n", FILE_APPEND);

        // Filter by arena_ids and game_mode_ids in PHP (JSON_CONTAINS varies by MySQL version)
        return array_filter($seasons, function ($season) use ($arenaId, $gameMode) {
            if ($season['arena_ids'] !== null) {
                $arenaIds = json_decode($season['arena_ids'], true);
                if (is_array($arenaIds) && !in_array($arenaId, $arenaIds)) {
                    return false;
                }
            }
            if ($season['game_mode_ids'] !== null) {
                $gameModeIds = json_decode($season['game_mode_ids'], true);
                if (is_array($gameModeIds) && !in_array($gameMode, $gameModeIds)) {
                    return false;
                }
            }
            return true;
        });
    }

    public function addSeasonMatch(int $seasonId, int $matchId): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('INSERT IGNORE INTO season_matches (season_id, match_id) VALUES (:sid, :mid)');
        $stmt->execute(['sid' => $seasonId, 'mid' => $matchId]);
    }

    // ==========================================
    // Enrollment
    // ==========================================

    public function addParticipant(int $seasonId, string $playerUuid): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('INSERT IGNORE INTO season_participants (season_id, player_uuid) VALUES (:sid, :uuid)');
        $stmt->execute(['sid' => $seasonId, 'uuid' => $playerUuid]);
    }

    public function isParticipant(int $seasonId, string $playerUuid): bool
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT 1 FROM season_participants WHERE season_id = :sid AND player_uuid = :uuid');
        $stmt->execute(['sid' => $seasonId, 'uuid' => $playerUuid]);
        return (bool) $stmt->fetchColumn();
    }

    public function getParticipantCount(int $seasonId): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT COUNT(*) FROM season_participants WHERE season_id = :sid');
        $stmt->execute(['sid' => $seasonId]);
        return (int) $stmt->fetchColumn();
    }

    // ==========================================
    // Stats
    // ==========================================

    public function updateSeasonStats(int $seasonId, string $playerUuid, string $arenaId, array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO season_player_stats
                (season_id, player_uuid, arena_id, matches_played, matches_won, matches_lost,
                 pvp_kills, pvp_deaths, pve_kills, pve_deaths,
                 damage_dealt, damage_taken, total_time_played, best_waves_survived, ranking_points)
            VALUES
                (:sid, :uuid, :arena_id, :played, :won, :lost,
                 :pvp_kills, :pvp_deaths, :pve_kills, :pve_deaths,
                 :dmg_dealt, :dmg_taken, :time_played, :waves, :points)
            ON DUPLICATE KEY UPDATE
                matches_played = matches_played + VALUES(matches_played),
                matches_won = matches_won + VALUES(matches_won),
                matches_lost = matches_lost + VALUES(matches_lost),
                pvp_kills = pvp_kills + VALUES(pvp_kills),
                pvp_deaths = pvp_deaths + VALUES(pvp_deaths),
                pve_kills = pve_kills + VALUES(pve_kills),
                pve_deaths = pve_deaths + VALUES(pve_deaths),
                damage_dealt = damage_dealt + VALUES(damage_dealt),
                damage_taken = damage_taken + VALUES(damage_taken),
                total_time_played = total_time_played + VALUES(total_time_played),
                best_waves_survived = GREATEST(COALESCE(best_waves_survived, 0), COALESCE(VALUES(best_waves_survived), 0)),
                ranking_points = ranking_points + VALUES(ranking_points)
        ');
        $stmt->execute([
            'sid' => $seasonId,
            'uuid' => $playerUuid,
            'arena_id' => $arenaId,
            'played' => $data['matches_played'] ?? 0,
            'won' => $data['matches_won'] ?? 0,
            'lost' => $data['matches_lost'] ?? 0,
            'pvp_kills' => $data['pvp_kills'] ?? 0,
            'pvp_deaths' => $data['pvp_deaths'] ?? 0,
            'pve_kills' => $data['pve_kills'] ?? 0,
            'pve_deaths' => $data['pve_deaths'] ?? 0,
            'dmg_dealt' => $data['damage_dealt'] ?? 0,
            'dmg_taken' => $data['damage_taken'] ?? 0,
            'time_played' => $data['time_played'] ?? 0,
            'waves' => $data['waves_survived'] ?? null,
            'points' => $data['ranking_points'] ?? 0,
        ]);
    }

    // ==========================================
    // Leaderboard (Live)
    // ==========================================

    public function getSeasonLeaderboard(int $seasonId, string $rankingMode, string $order = 'DESC', int $limit = 25, int $offset = 0, int $minMatches = 0): array
    {
        $allowedSorts = ['wins' => 'matches_won', 'win_rate' => 'win_rate', 'pvp_kills' => 'pvp_kills', 'pvp_kd_ratio' => 'pvp_kd_ratio', 'points' => 'ranking_points'];
        $sortCol = $allowedSorts[$rankingMode] ?? 'matches_won';
        $order = strtoupper($order) === 'ASC' ? 'ASC' : 'DESC';

        $db = Database::getConnection();
        $sql = "
            SELECT sg.*, p.username,
                   ROW_NUMBER() OVER (ORDER BY sg.{$sortCol} {$order}) AS rank_position
            FROM season_player_global_stats sg
            JOIN players p ON sg.player_uuid = p.uuid
            WHERE sg.season_id = :sid AND sg.matches_played >= :min_matches
            ORDER BY sg.{$sortCol} {$order}
            LIMIT :limit OFFSET :offset
        ";
        $stmt = $db->prepare($sql);
        $stmt->bindValue('sid', $seasonId, PDO::PARAM_INT);
        $stmt->bindValue('min_matches', $minMatches, PDO::PARAM_INT);
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function getSeasonLeaderboardCount(int $seasonId, int $minMatches = 0): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT COUNT(*) FROM season_player_global_stats
            WHERE season_id = :sid AND matches_played >= :min_matches
        ');
        $stmt->bindValue('sid', $seasonId, PDO::PARAM_INT);
        $stmt->bindValue('min_matches', $minMatches, PDO::PARAM_INT);
        $stmt->execute();
        return (int) $stmt->fetchColumn();
    }

    // ==========================================
    // Freeze Rankings
    // ==========================================

    public function freezeRankings(int $seasonId, string $rankingMode): void
    {
        $allowedSorts = ['wins' => 'matches_won', 'win_rate' => 'win_rate', 'pvp_kills' => 'pvp_kills', 'pvp_kd_ratio' => 'pvp_kd_ratio', 'points' => 'ranking_points'];
        $sortCol = $allowedSorts[$rankingMode] ?? 'matches_won';

        $db = Database::getConnection();

        // Clear any existing frozen rankings for this season
        $stmt = $db->prepare('DELETE FROM season_rankings WHERE season_id = :sid');
        $stmt->execute(['sid' => $seasonId]);

        // Insert from the global stats view, ordered by ranking mode
        $sql = "
            INSERT INTO season_rankings
                (season_id, player_uuid, rank_position, matches_played, matches_won, matches_lost,
                 pvp_kills, pvp_deaths, pve_kills, pve_deaths, pvp_kd_ratio, win_rate,
                 ranking_points, ranking_value)
            SELECT
                sg.season_id, sg.player_uuid,
                ROW_NUMBER() OVER (ORDER BY sg.{$sortCol} DESC),
                sg.matches_played, sg.matches_won, sg.matches_lost,
                sg.pvp_kills, sg.pvp_deaths, sg.pve_kills, sg.pve_deaths,
                sg.pvp_kd_ratio, sg.win_rate, sg.ranking_points,
                sg.{$sortCol}
            FROM season_player_global_stats sg
            WHERE sg.season_id = :sid AND sg.matches_played > 0
            ORDER BY sg.{$sortCol} DESC
        ";
        $stmt = $db->prepare($sql);
        $stmt->execute(['sid' => $seasonId]);
    }

    // ==========================================
    // Frozen Rankings
    // ==========================================

    public function getFrozenRankings(int $seasonId, int $limit = 25, int $offset = 0): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT sr.*, p.username
            FROM season_rankings sr
            JOIN players p ON sr.player_uuid = p.uuid
            WHERE sr.season_id = :sid
            ORDER BY sr.rank_position ASC
            LIMIT :limit OFFSET :offset
        ');
        $stmt->bindValue('sid', $seasonId, PDO::PARAM_INT);
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function getFrozenRankingsCount(int $seasonId): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT COUNT(*) FROM season_rankings WHERE season_id = :sid');
        $stmt->execute(['sid' => $seasonId]);
        return (int) $stmt->fetchColumn();
    }

    // ==========================================
    // Player History
    // ==========================================

    public function getPlayerSeasonHistory(string $playerUuid): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT s.id, s.name, s.slug, s.status, s.starts_at, s.ends_at, s.ranking_mode,
                   sr.rank_position, sr.matches_played, sr.matches_won, sr.pvp_kills,
                   sr.pvp_kd_ratio, sr.win_rate, sr.ranking_points, sr.ranking_value,
                   (SELECT COUNT(*) FROM season_rankings sr2 WHERE sr2.season_id = s.id) AS total_participants
            FROM season_rankings sr
            JOIN seasons s ON sr.season_id = s.id
            WHERE sr.player_uuid = :uuid
            ORDER BY s.ends_at DESC
        ');
        $stmt->execute(['uuid' => $playerUuid]);
        $history = $stmt->fetchAll();

        // Also include active seasons the player participates in (not yet frozen)
        $stmt2 = $db->prepare('
            SELECT s.id, s.name, s.slug, s.status, s.starts_at, s.ends_at, s.ranking_mode,
                   sg.matches_played, sg.matches_won, sg.pvp_kills,
                   sg.pvp_kd_ratio, sg.win_rate, sg.ranking_points,
                   (SELECT COUNT(*) FROM season_participants sp2 WHERE sp2.season_id = s.id) AS total_participants
            FROM season_participants sp
            JOIN seasons s ON sp.season_id = s.id
            LEFT JOIN season_player_global_stats sg ON sg.season_id = s.id AND sg.player_uuid = :uuid2
            WHERE sp.player_uuid = :uuid AND s.status = :status
            ORDER BY s.ends_at ASC
        ');
        $stmt2->execute(['uuid' => $playerUuid, 'uuid2' => $playerUuid, 'status' => 'active']);
        $active = $stmt2->fetchAll();

        return array_merge($active, $history);
    }
}
