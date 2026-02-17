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
            ORDER BY n.is_pinned DESC, n.created_at DESC
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
            INSERT INTO server_notifications (title, message, image_path, is_pinned, type, is_active, starts_at, ends_at, created_by)
            VALUES (:title, :message, :image_path, :is_pinned, :type, :is_active, :starts_at, :ends_at, :created_by)
        ');
        $stmt->execute([
            'title' => $data['title'],
            'message' => $data['message'],
            'image_path' => $data['image_path'] ?? null,
            'is_pinned' => $data['is_pinned'] ?? 0,
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

        $fields = 'title = :title, message = :message, type = :type,
                    is_active = :is_active, is_pinned = :is_pinned,
                    starts_at = :starts_at, ends_at = :ends_at';
        $params = [
            'id' => $id,
            'title' => $data['title'],
            'message' => $data['message'],
            'type' => $data['type'] ?? 'info',
            'is_active' => $data['is_active'] ?? 1,
            'is_pinned' => $data['is_pinned'] ?? 0,
            'starts_at' => $data['starts_at'] ?: null,
            'ends_at' => $data['ends_at'] ?: null,
        ];

        if (array_key_exists('image_path', $data)) {
            $fields .= ', image_path = :image_path';
            $params['image_path'] = $data['image_path'];
        }

        $stmt = $db->prepare("UPDATE server_notifications SET {$fields} WHERE id = :id");
        $stmt->execute($params);
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
            ORDER BY is_pinned DESC, created_at DESC
        ')->fetchAll();
    }

    public function getActivePaginated(int $limit, int $offset): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT n.*, a.username AS author_name
            FROM server_notifications n
            LEFT JOIN admin_users a ON n.created_by = a.id
            WHERE n.is_active = 1
            AND (n.starts_at IS NULL OR n.starts_at <= NOW())
            AND (n.ends_at IS NULL OR n.ends_at >= NOW())
            ORDER BY n.is_pinned DESC, n.created_at DESC
            LIMIT :limit OFFSET :offset
        ');
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue('offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }

    public function getActiveCount(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('
            SELECT COUNT(*) FROM server_notifications
            WHERE is_active = 1
            AND (starts_at IS NULL OR starts_at <= NOW())
            AND (ends_at IS NULL OR ends_at >= NOW())
        ')->fetchColumn();
    }

    public function getLatestActive(int $limit = 3): array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT n.*, a.username AS author_name
            FROM server_notifications n
            LEFT JOIN admin_users a ON n.created_by = a.id
            WHERE n.is_active = 1
            AND (n.starts_at IS NULL OR n.starts_at <= NOW())
            AND (n.ends_at IS NULL OR n.ends_at >= NOW())
            ORDER BY n.created_at DESC
            LIMIT :limit
        ');
        $stmt->bindValue('limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll();
    }
}
