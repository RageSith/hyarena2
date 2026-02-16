<?php

namespace App\Repository;

use App\Database;
use PDO;

class StatsRepository
{
    /**
     * Upserts per-arena stats for a player.
     * Global stats are derived from the player_global_stats view — no NULL arena_id rows.
     */
    public function updateStats(string $playerUuid, string $arenaId, array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO player_stats
                (player_uuid, arena_id, matches_played, matches_won, matches_lost,
                 pvp_kills, pvp_deaths, pve_kills, pve_deaths,
                 damage_dealt, damage_taken, total_time_played, best_waves_survived)
            VALUES
                (:uuid, :arena_id, :played, :won, :lost,
                 :pvp_kills, :pvp_deaths, :pve_kills, :pve_deaths,
                 :dmg_dealt, :dmg_taken, :time_played, :waves)
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
                best_waves_survived = GREATEST(COALESCE(best_waves_survived, 0), COALESCE(VALUES(best_waves_survived), 0))
        ');
        $stmt->execute([
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
        ]);
    }

    public function updateKitStats(string $playerUuid, string $kitId, array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO player_kit_stats
                (player_uuid, kit_id, matches_played, matches_won,
                 pvp_kills, pvp_deaths, pve_kills, pve_deaths,
                 damage_dealt, damage_taken)
            VALUES
                (:uuid, :kit_id, :played, :won,
                 :pvp_kills, :pvp_deaths, :pve_kills, :pve_deaths,
                 :dmg_dealt, :dmg_taken)
            ON DUPLICATE KEY UPDATE
                matches_played = matches_played + VALUES(matches_played),
                matches_won = matches_won + VALUES(matches_won),
                pvp_kills = pvp_kills + VALUES(pvp_kills),
                pvp_deaths = pvp_deaths + VALUES(pvp_deaths),
                pve_kills = pve_kills + VALUES(pve_kills),
                pve_deaths = pve_deaths + VALUES(pve_deaths),
                damage_dealt = damage_dealt + VALUES(damage_dealt),
                damage_taken = damage_taken + VALUES(damage_taken)
        ');
        $stmt->execute([
            'uuid' => $playerUuid,
            'kit_id' => $kitId,
            'played' => $data['matches_played'] ?? 0,
            'won' => $data['matches_won'] ?? 0,
            'pvp_kills' => $data['pvp_kills'] ?? 0,
            'pvp_deaths' => $data['pvp_deaths'] ?? 0,
            'pve_kills' => $data['pve_kills'] ?? 0,
            'pve_deaths' => $data['pve_deaths'] ?? 0,
            'dmg_dealt' => $data['damage_dealt'] ?? 0,
            'dmg_taken' => $data['damage_taken'] ?? 0,
        ]);
    }

    public function getKitStats(string $playerUuid): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT
                pks.*,
                k.display_name AS kit_name,
                ROUND(pks.matches_played / GREATEST(
                    (SELECT SUM(matches_played) FROM player_kit_stats WHERE player_uuid = :uuid2), 1
                ) * 100, 1) AS usage_pct
            FROM player_kit_stats pks
            JOIN kits k ON pks.kit_id = k.id
            WHERE pks.player_uuid = :uuid
            ORDER BY pks.matches_played DESC
        ');
        $stmt->execute(['uuid' => $playerUuid, 'uuid2' => $playerUuid]);
        return $stmt->fetchAll();
    }

    public function getTotalKills(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('SELECT COALESCE(SUM(pvp_kills) + SUM(pve_kills), 0) FROM match_participants')->fetchColumn();
    }

    public function getGlobalStats(string $playerUuid): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM player_global_stats WHERE player_uuid = :uuid');
        $stmt->execute(['uuid' => $playerUuid]);
        return $stmt->fetch() ?: null;
    }

    public function getArenaStats(string $playerUuid): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT ps.*, a.display_name AS arena_name, a.game_mode
            FROM player_stats ps
            JOIN arenas a ON ps.arena_id = a.id
            WHERE ps.player_uuid = :uuid
            ORDER BY ps.matches_played DESC
        ');
        $stmt->execute(['uuid' => $playerUuid]);
        return $stmt->fetchAll();
    }

    public function getLeaderboard(string $sort = 'pvp_kills', string $order = 'DESC', int $limit = 25, int $offset = 0, ?string $arenaId = null): array
    {
        $allowedSorts = ['pvp_kills', 'matches_won', 'pvp_kd_ratio', 'win_rate', 'pve_kills', 'matches_played', 'best_waves_survived'];
        if (!in_array($sort, $allowedSorts)) {
            $sort = 'pvp_kills';
        }
        $order = strtoupper($order) === 'ASC' ? 'ASC' : 'DESC';

        $db = Database::getConnection();

        if ($arenaId !== null) {
            // Per-arena leaderboard from the table
            $sql = "
                SELECT ps.*, p.username,
                       ROW_NUMBER() OVER (ORDER BY ps.{$sort} {$order}) AS rank_position
                FROM player_stats ps
                JOIN players p ON ps.player_uuid = p.uuid
                WHERE ps.arena_id = :arena_id AND ps.matches_played > 0
                ORDER BY ps.{$sort} {$order}
                LIMIT :limit OFFSET :offset
            ";
            $stmt = $db->prepare($sql);
            $stmt->bindValue('arena_id', $arenaId);
        } else {
            // Global leaderboard from the view
            $sql = "
                SELECT ps.*, p.username,
                       ROW_NUMBER() OVER (ORDER BY ps.{$sort} {$order}) AS rank_position
                FROM player_global_stats ps
                JOIN players p ON ps.player_uuid = p.uuid
                WHERE ps.matches_played > 0
                ORDER BY ps.{$sort} {$order}
                LIMIT :limit OFFSET :offset
            ";
            $stmt = $db->prepare($sql);
        }

        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function getLeaderboardCount(?string $arenaId = null): int
    {
        $db = Database::getConnection();

        if ($arenaId !== null) {
            $sql = "SELECT COUNT(*) FROM player_stats WHERE arena_id = :arena_id AND matches_played > 0";
            $stmt = $db->prepare($sql);
            $stmt->bindValue('arena_id', $arenaId);
        } else {
            $sql = "SELECT COUNT(*) FROM player_global_stats WHERE matches_played > 0";
            $stmt = $db->prepare($sql);
        }

        $stmt->execute();
        return (int) $stmt->fetchColumn();
    }

    public function getLeaderboardByGameMode(string $gameMode, string $sort = 'pvp_kills', string $order = 'DESC', int $limit = 25, int $offset = 0): array
    {
        $allowedSorts = ['pvp_kills', 'matches_won', 'pvp_kd_ratio', 'win_rate', 'pve_kills', 'matches_played', 'best_waves_survived'];
        if (!in_array($sort, $allowedSorts)) {
            $sort = 'pvp_kills';
        }
        $order = strtoupper($order) === 'ASC' ? 'ASC' : 'DESC';

        $db = Database::getConnection();

        $sql = "
            SELECT
                p.username,
                ps.player_uuid,
                SUM(ps.pvp_kills) AS pvp_kills,
                SUM(ps.pvp_deaths) AS pvp_deaths,
                CASE WHEN SUM(ps.pvp_deaths) > 0
                     THEN ROUND(SUM(ps.pvp_kills) / SUM(ps.pvp_deaths), 2)
                     ELSE SUM(ps.pvp_kills)
                END AS pvp_kd_ratio,
                SUM(ps.matches_won) AS matches_won,
                CASE WHEN SUM(ps.matches_played) > 0
                     THEN ROUND(SUM(ps.matches_won) / SUM(ps.matches_played) * 100, 1)
                     ELSE 0
                END AS win_rate,
                SUM(ps.pve_kills) AS pve_kills,
                SUM(ps.pve_deaths) AS pve_deaths,
                MAX(ps.best_waves_survived) AS best_waves_survived,
                SUM(ps.matches_played) AS matches_played,
                ROW_NUMBER() OVER (ORDER BY {$sort} {$order}) AS rank_position
            FROM player_stats ps
            JOIN arenas a ON ps.arena_id = a.id
            JOIN players p ON ps.player_uuid = p.uuid
            WHERE a.game_mode = :game_mode AND ps.matches_played > 0
            GROUP BY ps.player_uuid, p.username
            ORDER BY {$sort} {$order}
            LIMIT :limit OFFSET :offset
        ";
        $stmt = $db->prepare($sql);
        $stmt->bindValue('game_mode', $gameMode);
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function getLeaderboardCountByGameMode(string $gameMode): int
    {
        $db = Database::getConnection();
        $sql = "
            SELECT COUNT(DISTINCT ps.player_uuid)
            FROM player_stats ps
            JOIN arenas a ON ps.arena_id = a.id
            WHERE a.game_mode = :game_mode AND ps.matches_played > 0
        ";
        $stmt = $db->prepare($sql);
        $stmt->bindValue('game_mode', $gameMode);
        $stmt->execute();
        return (int) $stmt->fetchColumn();
    }

    public function getPlayerRecentMatches(string $playerUuid, int $limit = 10): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT mp.*, m.arena_id, m.game_mode, m.duration_seconds, m.ended_at,
                   a.display_name AS arena_name, a.icon AS arena_icon, w.username AS winner_name
            FROM match_participants mp
            JOIN matches m ON mp.match_id = m.id
            JOIN arenas a ON m.arena_id = a.id
            LEFT JOIN players w ON m.winner_uuid = w.uuid
            WHERE mp.player_uuid = :uuid
            ORDER BY m.ended_at DESC
            LIMIT :limit
        ');
        $stmt->bindValue('uuid', $playerUuid);
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }
}
