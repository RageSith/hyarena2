<?php

namespace App\Repository;

use App\Database;
use PDO;

class BugReportRepository
{
    public function getAll(?string $status = null, int $limit = 20, int $offset = 0): array
    {
        $db = Database::getConnection();

        $where = '';
        $params = [];
        if ($status) {
            $where = 'WHERE status = :status';
            $params['status'] = $status;
        }

        $stmt = $db->prepare("
            SELECT * FROM bug_reports $where
            ORDER BY created_at DESC
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

    public function getCount(?string $status = null): int
    {
        $db = Database::getConnection();

        $where = '';
        $params = [];
        if ($status) {
            $where = 'WHERE status = :status';
            $params['status'] = $status;
        }

        $stmt = $db->prepare("SELECT COUNT(*) FROM bug_reports $where");
        $stmt->execute($params);
        return (int) $stmt->fetchColumn();
    }

    public function findById(int $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM bug_reports WHERE id = :id');
        $stmt->execute(['id' => $id]);
        return $stmt->fetch() ?: null;
    }

    public function updateStatus(int $id, string $status, ?string $adminNotes, ?int $resolvedBy): void
    {
        $db = Database::getConnection();

        $resolvedAt = in_array($status, ['resolved', 'closed']) ? date('Y-m-d H:i:s') : null;

        $stmt = $db->prepare('
            UPDATE bug_reports
            SET status = :status,
                admin_notes = :admin_notes,
                resolved_by = :resolved_by,
                resolved_at = :resolved_at
            WHERE id = :id
        ');
        $stmt->execute([
            'status' => $status,
            'admin_notes' => $adminNotes,
            'resolved_by' => $resolvedBy,
            'resolved_at' => $resolvedAt,
            'id' => $id,
        ]);
    }

    public function delete(int $id): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM bug_reports WHERE id = :id');
        $stmt->execute(['id' => $id]);
    }

    public function getCountByStatus(): array
    {
        $db = Database::getConnection();
        $stmt = $db->query("
            SELECT status, COUNT(*) as count
            FROM bug_reports
            GROUP BY status
        ");
        $rows = $stmt->fetchAll();

        $counts = ['open' => 0, 'acknowledged' => 0, 'resolved' => 0, 'closed' => 0];
        foreach ($rows as $row) {
            $counts[$row['status']] = (int) $row['count'];
        }
        return $counts;
    }
}
