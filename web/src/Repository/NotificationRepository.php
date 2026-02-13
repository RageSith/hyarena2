<?php

namespace App\Repository;

use App\Database;
use PDO;

class NotificationRepository
{
    public function getAll(): array
    {
        $db = Database::getConnection();
        return $db->query('
            SELECT n.*, a.username AS created_by_name
            FROM server_notifications n
            LEFT JOIN admin_users a ON n.created_by = a.id
            ORDER BY n.created_at DESC
        ')->fetchAll();
    }

    public function findById(int $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM server_notifications WHERE id = :id');
        $stmt->execute(['id' => $id]);
        return $stmt->fetch() ?: null;
    }

    public function create(array $data): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO server_notifications (title, message, type, is_active, starts_at, ends_at, created_by)
            VALUES (:title, :message, :type, :is_active, :starts_at, :ends_at, :created_by)
        ');
        $stmt->execute([
            'title' => $data['title'],
            'message' => $data['message'],
            'type' => $data['type'] ?? 'info',
            'is_active' => $data['is_active'] ?? 1,
            'starts_at' => $data['starts_at'] ?: null,
            'ends_at' => $data['ends_at'] ?: null,
            'created_by' => $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public function update(int $id, array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            UPDATE server_notifications
            SET title = :title, message = :message, type = :type,
                is_active = :is_active, starts_at = :starts_at, ends_at = :ends_at
            WHERE id = :id
        ');
        $stmt->execute([
            'id' => $id,
            'title' => $data['title'],
            'message' => $data['message'],
            'type' => $data['type'] ?? 'info',
            'is_active' => $data['is_active'] ?? 1,
            'starts_at' => $data['starts_at'] ?: null,
            'ends_at' => $data['ends_at'] ?: null,
        ]);
    }

    public function delete(int $id): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM server_notifications WHERE id = :id');
        $stmt->execute(['id' => $id]);
    }

    public function getActive(): array
    {
        $db = Database::getConnection();
        return $db->query('
            SELECT * FROM server_notifications
            WHERE is_active = 1
            AND (starts_at IS NULL OR starts_at <= NOW())
            AND (ends_at IS NULL OR ends_at >= NOW())
            ORDER BY created_at DESC
        ')->fetchAll();
    }
}
