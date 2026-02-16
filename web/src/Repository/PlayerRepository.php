<?php

namespace App\Repository;

use App\Database;
use PDO;

class PlayerRepository
{
    public function upsert(string $uuid, string $username): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO players (uuid, username)
            VALUES (:uuid, :username)
            ON DUPLICATE KEY UPDATE username = :username2, last_seen = CURRENT_TIMESTAMP
        ');
        $stmt->execute([
            'uuid' => $uuid,
            'username' => $username,
            'username2' => $username,
        ]);
    }

    public function findByUsername(string $username): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM players WHERE username = :username');
        $stmt->execute(['username' => $username]);
        return $stmt->fetch() ?: null;
    }

    public function findByUuid(string $uuid): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM players WHERE uuid = :uuid');
        $stmt->execute(['uuid' => $uuid]);
        return $stmt->fetch() ?: null;
    }

    public function getTotalCount(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('SELECT COUNT(*) FROM players')->fetchColumn();
    }

    public function updateCurrency(string $uuid, int $amount): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE players SET currency = currency + :amount WHERE uuid = :uuid');
        $stmt->execute(['amount' => $amount, 'uuid' => $uuid]);
    }

    public function updateHonor(string $uuid, int $honor, string $rank): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE players SET honor = :honor, honor_rank = :rank WHERE uuid = :uuid');
        $stmt->execute(['honor' => $honor, 'rank' => $rank, 'uuid' => $uuid]);
    }

    public function updateEconomy(string $uuid, int $arenaPoints, int $honor, string $honorRank): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            UPDATE players
            SET arena_points = :ap, honor = :honor, honor_rank = :rank
            WHERE uuid = :uuid
        ');
        $stmt->execute([
            'ap' => $arenaPoints,
            'honor' => $honor,
            'rank' => $honorRank,
            'uuid' => $uuid,
        ]);
    }

    public function batchUpdateEconomy(array $players): int
    {
        if (empty($players)) {
            return 0;
        }

        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO players (uuid, username, arena_points, honor, honor_rank)
            VALUES (:uuid, :username, :ap, :honor, :rank)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                arena_points = VALUES(arena_points),
                honor = VALUES(honor),
                honor_rank = VALUES(honor_rank),
                last_seen = CURRENT_TIMESTAMP
        ');

        $updated = 0;
        foreach ($players as $p) {
            $stmt->execute([
                'uuid' => $p['uuid'],
                'username' => $p['username'],
                'ap' => (int) ($p['arena_points'] ?? 0),
                'honor' => (int) ($p['honor'] ?? 0),
                'rank' => $p['honor_rank'] ?? 'Unranked',
            ]);
            $updated++;
        }

        return $updated;
    }

    // ==========================================
    // Admin: Player Management
    // ==========================================

    public function searchPlayers(?string $query = null, int $limit = 20, int $offset = 0): array
    {
        $db = Database::getConnection();

        $where = '';
        $params = [];
        if ($query) {
            $where = 'WHERE username LIKE :query OR uuid LIKE :query2';
            $params['query'] = '%' . $query . '%';
            $params['query2'] = '%' . $query . '%';
        }

        $stmt = $db->prepare("
            SELECT * FROM players $where
            ORDER BY last_seen DESC
            LIMIT :limit OFFSET :offset
        ");
        foreach ($params as $k => $v) {
            $stmt->bindValue($k, $v);
        }
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();

        return $stmt->fetchAll();
    }

    public function getPlayerCount(?string $query = null): int
    {
        $db = Database::getConnection();

        $where = '';
        $params = [];
        if ($query) {
            $where = 'WHERE username LIKE :query OR uuid LIKE :query2';
            $params['query'] = '%' . $query . '%';
            $params['query2'] = '%' . $query . '%';
        }

        $stmt = $db->prepare("SELECT COUNT(*) FROM players $where");
        $stmt->execute($params);
        return (int) $stmt->fetchColumn();
    }

    public function getPlayerDetail(string $uuid): ?array
    {
        $db = Database::getConnection();

        // Player info
        $stmt = $db->prepare('SELECT * FROM players WHERE uuid = :uuid');
        $stmt->execute(['uuid' => $uuid]);
        $player = $stmt->fetch();
        if (!$player) return null;

        // Linked account
        $stmt = $db->prepare('SELECT * FROM linked_accounts WHERE player_uuid = :uuid');
        $stmt->execute(['uuid' => $uuid]);
        $player['linked_account'] = $stmt->fetch() ?: null;

        // Global stats (aggregated view)
        $stmt = $db->prepare('SELECT * FROM player_global_stats WHERE player_uuid = :uuid');
        $stmt->execute(['uuid' => $uuid]);
        $player['global_stats'] = $stmt->fetch() ?: null;

        // Recent matches
        $stmt = $db->prepare('
            SELECT m.*, mp.pvp_kills, mp.pvp_deaths, mp.damage_dealt, mp.is_winner
            FROM match_participants mp
            JOIN matches m ON mp.match_id = m.id
            WHERE mp.player_uuid = :uuid
            ORDER BY m.ended_at DESC
            LIMIT 10
        ');
        $stmt->execute(['uuid' => $uuid]);
        $player['recent_matches'] = $stmt->fetchAll();

        return $player;
    }

    public function banPlayer(string $uuid, string $reason, int $bannedBy): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            UPDATE players
            SET is_banned = 1, ban_reason = :reason, banned_at = NOW(), banned_by = :banned_by
            WHERE uuid = :uuid
        ');
        $stmt->execute([
            'reason' => $reason,
            'banned_by' => $bannedBy,
            'uuid' => $uuid,
        ]);
    }

    public function unbanPlayer(string $uuid): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            UPDATE players
            SET is_banned = 0, ban_reason = NULL, banned_at = NULL, banned_by = NULL
            WHERE uuid = :uuid
        ');
        $stmt->execute(['uuid' => $uuid]);
    }
}
