<?php

namespace App\Repository;

use App\Database;
use PDO;

class MatchRepository
{
    public function create(string $arenaId, string $gameMode, ?string $winnerUuid, int $durationSeconds): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO matches (arena_id, game_mode, winner_uuid, duration_seconds, ended_at)
            VALUES (:arena_id, :game_mode, :winner_uuid, :duration, NOW())
        ');
        $stmt->execute([
            'arena_id' => $arenaId,
            'game_mode' => $gameMode,
            'winner_uuid' => $winnerUuid,
            'duration' => $durationSeconds,
        ]);
        return (int) $db->lastInsertId();
    }

    public function getTotalCount(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('SELECT COUNT(*) FROM matches')->fetchColumn();
    }

    public function getMatchesToday(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('SELECT COUNT(*) FROM matches WHERE DATE(ended_at) = CURDATE()')->fetchColumn();
    }

    public function getRecentMatches(int $limit = 10, int $offset = 0): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT m.*, a.display_name AS arena_name,
                   p.username AS winner_name,
                   (SELECT COUNT(*) FROM match_participants mp WHERE mp.match_id = m.id) AS participant_count,
                   wp.pvp_kills AS winner_kills,
                   wp.is_bot AS winner_is_bot,
                   wp.bot_name AS winner_bot_name
            FROM matches m
            JOIN arenas a ON m.arena_id = a.id
            LEFT JOIN players p ON m.winner_uuid = p.uuid
            LEFT JOIN match_participants wp ON wp.match_id = m.id AND wp.is_winner = 1
            ORDER BY m.ended_at DESC
            LIMIT :limit OFFSET :offset
        ');
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function getMatchWithParticipants(int $matchId): ?array
    {
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT m.*, a.display_name AS arena_name, p.username AS winner_name
            FROM matches m
            JOIN arenas a ON m.arena_id = a.id
            LEFT JOIN players p ON m.winner_uuid = p.uuid
            WHERE m.id = :id
        ');
        $stmt->execute(['id' => $matchId]);
        $match = $stmt->fetch();
        if (!$match) return null;

        $stmt = $db->prepare('
            SELECT mp.*, p.username
            FROM match_participants mp
            LEFT JOIN players p ON mp.player_uuid = p.uuid
            WHERE mp.match_id = :match_id
            ORDER BY mp.is_winner DESC, mp.pvp_kills DESC
        ');
        $stmt->execute(['match_id' => $matchId]);
        $match['participants'] = $stmt->fetchAll();

        return $match;
    }
}
