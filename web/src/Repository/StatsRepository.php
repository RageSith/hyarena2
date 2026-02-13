<?php

namespace App\Repository;

use App\Database;
use PDO;

class StatsRepository
{
    public function updateStats(string $playerUuid, ?string $arenaId, array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO player_stats
                (player_uuid, arena_id, matches_played, matches_won, matches_lost,
                 pvp_kills, pvp_deaths, pve_kills, pve_deaths,
                 damage_dealt, damage_taken, total_time_played)
            VALUES
                (:uuid, :arena_id, :played, :won, :lost,
                 :pvp_kills, :pvp_deaths, :pve_kills, :pve_deaths,
                 :dmg_dealt, :dmg_taken, :time_played)
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
                total_time_played = total_time_played + VALUES(total_time_played)
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
        ]);
    }

    public function getGlobalStats(string $playerUuid): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM player_stats WHERE player_uuid = :uuid AND arena_id IS NULL');
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
        $allowedSorts = ['pvp_kills', 'matches_won', 'pvp_kd_ratio', 'win_rate', 'pve_kills', 'matches_played'];
        if (!in_array($sort, $allowedSorts)) {
            $sort = 'pvp_kills';
        }
        $order = strtoupper($order) === 'ASC' ? 'ASC' : 'DESC';

        $db = Database::getConnection();

        $where = $arenaId !== null ? 'ps.arena_id = :arena_id' : 'ps.arena_id IS NULL';

        $sql = "
            SELECT ps.*, p.username,
                   ROW_NUMBER() OVER (ORDER BY ps.{$sort} {$order}) AS rank_position
            FROM player_stats ps
            JOIN players p ON ps.player_uuid = p.uuid
            WHERE {$where} AND ps.matches_played > 0
            ORDER BY ps.{$sort} {$order}
            LIMIT :limit OFFSET :offset
        ";

        $stmt = $db->prepare($sql);
        if ($arenaId !== null) {
            $stmt->bindValue('arena_id', $arenaId);
        }
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function getLeaderboardCount(?string $arenaId = null): int
    {
        $db = Database::getConnection();
        $where = $arenaId !== null ? 'arena_id = :arena_id' : 'arena_id IS NULL';
        $sql = "SELECT COUNT(*) FROM player_stats WHERE {$where} AND matches_played > 0";
        $stmt = $db->prepare($sql);
        if ($arenaId !== null) {
            $stmt->bindValue('arena_id', $arenaId);
        }
        $stmt->execute();
        return (int) $stmt->fetchColumn();
    }

    public function getPlayerRecentMatches(string $playerUuid, int $limit = 10): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT mp.*, m.arena_id, m.game_mode, m.duration_seconds, m.ended_at,
                   a.display_name AS arena_name, w.username AS winner_name
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
