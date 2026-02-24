<?php

namespace App\Repository;

use App\Database;
use PDO;

class TeamRepository
{
    public function getAll(): array
    {
        $db = Database::getConnection();
        return $db->query('SELECT * FROM team_members ORDER BY sort_order ASC, id ASC')->fetchAll();
    }

    public function findById(int $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM team_members WHERE id = :id');
        $stmt->execute(['id' => $id]);
        return $stmt->fetch() ?: null;
    }

    public function create(array $data): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO team_members (name, role, image_path, sort_order)
            VALUES (:name, :role, :image_path, :sort_order)
        ');
        $stmt->execute([
            'name' => $data['name'],
            'role' => $data['role'],
            'image_path' => $data['image_path'] ?? null,
            'sort_order' => $data['sort_order'] ?? 0,
        ]);
        return (int) $db->lastInsertId();
    }

    public function update(int $id, array $data): void
    {
        $db = Database::getConnection();

        $fields = 'name = :name, role = :role';
        $params = [
            'id' => $id,
            'name' => $data['name'],
            'role' => $data['role'],
        ];

        if (array_key_exists('image_path', $data)) {
            $fields .= ', image_path = :image_path';
            $params['image_path'] = $data['image_path'];
        }

        $stmt = $db->prepare("UPDATE team_members SET {$fields} WHERE id = :id");
        $stmt->execute($params);
    }

    public function delete(int $id): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM team_members WHERE id = :id');
        $stmt->execute(['id' => $id]);
    }

    public function getMaxSortOrder(): int
    {
        $db = Database::getConnection();
        return (int) $db->query('SELECT COALESCE(MAX(sort_order), 0) FROM team_members')->fetchColumn();
    }

    public function swapSortOrder(int $id1, int $id2): void
    {
        $db = Database::getConnection();
        $db->beginTransaction();
        try {
            $stmt = $db->prepare('SELECT id, sort_order FROM team_members WHERE id IN (:id1, :id2)');
            $stmt->execute(['id1' => $id1, 'id2' => $id2]);
            $rows = $stmt->fetchAll(PDO::FETCH_KEY_PAIR);

            if (count($rows) === 2) {
                $update = $db->prepare('UPDATE team_members SET sort_order = :sort_order WHERE id = :id');
                $update->execute(['sort_order' => $rows[$id2], 'id' => $id1]);
                $update->execute(['sort_order' => $rows[$id1], 'id' => $id2]);
            }

            $db->commit();
        } catch (\Exception $e) {
            $db->rollBack();
            throw $e;
        }
    }

    public function getAdjacentMember(int $currentSortOrder, string $direction): ?array
    {
        $db = Database::getConnection();

        if ($direction === 'up') {
            $stmt = $db->prepare('
                SELECT * FROM team_members
                WHERE sort_order < :sort_order
                ORDER BY sort_order DESC
                LIMIT 1
            ');
        } else {
            $stmt = $db->prepare('
                SELECT * FROM team_members
                WHERE sort_order > :sort_order
                ORDER BY sort_order ASC
                LIMIT 1
            ');
        }

        $stmt->execute(['sort_order' => $currentSortOrder]);
        return $stmt->fetch() ?: null;
    }
}
