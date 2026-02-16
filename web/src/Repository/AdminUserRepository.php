<?php

namespace App\Repository;

use App\Database;
use PDO;

class AdminUserRepository
{
    public function getAll(): array
    {
        $db = Database::getConnection();
        return $db->query('SELECT id, username, role, last_login, created_at FROM admin_users ORDER BY created_at ASC')->fetchAll();
    }

    public function findById(int $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT id, username, role, last_login, created_at FROM admin_users WHERE id = :id');
        $stmt->execute(['id' => $id]);
        return $stmt->fetch() ?: null;
    }

    public function findByUsername(string $username): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT id, username, role FROM admin_users WHERE username = :username');
        $stmt->execute(['username' => $username]);
        return $stmt->fetch() ?: null;
    }

    public function create(string $username, string $password, string $role): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO admin_users (username, password_hash, role)
            VALUES (:username, :password_hash, :role)
        ');
        $stmt->execute([
            'username' => $username,
            'password_hash' => password_hash($password, PASSWORD_DEFAULT),
            'role' => $role,
        ]);
        return (int) $db->lastInsertId();
    }

    public function update(int $id, array $data): void
    {
        $db = Database::getConnection();

        if (!empty($data['password'])) {
            $stmt = $db->prepare('UPDATE admin_users SET role = :role, password_hash = :password_hash WHERE id = :id');
            $stmt->execute([
                'role' => $data['role'],
                'password_hash' => password_hash($data['password'], PASSWORD_DEFAULT),
                'id' => $id,
            ]);
        } else {
            $stmt = $db->prepare('UPDATE admin_users SET role = :role WHERE id = :id');
            $stmt->execute([
                'role' => $data['role'],
                'id' => $id,
            ]);
        }
    }

    public function delete(int $id): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM admin_users WHERE id = :id');
        $stmt->execute(['id' => $id]);
    }

    public function countByRole(): array
    {
        $db = Database::getConnection();
        $stmt = $db->query('SELECT role, COUNT(*) as count FROM admin_users GROUP BY role');
        $rows = $stmt->fetchAll();

        $counts = [];
        foreach ($rows as $row) {
            $counts[$row['role']] = (int) $row['count'];
        }
        return $counts;
    }

    public function countSuperAdmins(): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT COUNT(*) FROM admin_users WHERE role = 'super_admin'");
        $stmt->execute();
        return (int) $stmt->fetchColumn();
    }
}
