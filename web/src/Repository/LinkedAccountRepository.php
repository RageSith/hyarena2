<?php

namespace App\Repository;

use App\Database;

class LinkedAccountRepository
{
    public function findByEmail(string $email): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM linked_accounts WHERE email = :email');
        $stmt->execute(['email' => $email]);
        return $stmt->fetch() ?: null;
    }

    public function findById(int $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM linked_accounts WHERE id = :id');
        $stmt->execute(['id' => $id]);
        return $stmt->fetch() ?: null;
    }

    public function findByPlayerUuid(string $uuid): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM linked_accounts WHERE player_uuid = :uuid');
        $stmt->execute(['uuid' => $uuid]);
        return $stmt->fetch() ?: null;
    }

    public function create(string $email, string $passwordHash): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('INSERT INTO linked_accounts (email, password_hash) VALUES (:email, :hash)');
        $stmt->execute(['email' => $email, 'hash' => $passwordHash]);
        return (int) $db->lastInsertId();
    }

    public function linkPlayer(int $accountId, string $playerUuid): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE linked_accounts SET player_uuid = :uuid, verified_at = NOW() WHERE id = :id');
        $stmt->execute(['uuid' => $playerUuid, 'id' => $accountId]);
    }

    public function storeLinkCode(string $code, string $playerUuid, string $expiresAt): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('INSERT INTO link_codes (code, player_uuid, expires_at) VALUES (:code, :uuid, :expires)');
        $stmt->execute(['code' => $code, 'uuid' => $playerUuid, 'expires' => $expiresAt]);
    }

    public function findValidCode(string $code): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT * FROM link_codes
            WHERE code = :code AND used = 0 AND expires_at > NOW()
        ');
        $stmt->execute(['code' => $code]);
        return $stmt->fetch() ?: null;
    }

    public function markCodeUsed(int $codeId): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE link_codes SET used = 1 WHERE id = :id');
        $stmt->execute(['id' => $codeId]);
    }
}
