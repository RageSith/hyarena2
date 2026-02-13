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
}
