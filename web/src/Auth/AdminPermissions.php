<?php

namespace App\Auth;

class AdminPermissions
{
    private const PERMISSIONS = [
        'super_admin' => [
            'dashboard', 'bug_reports', 'players',
            'notifications', 'seasons', 'webhooks', 'admin_users', 'servers',
            'data_management', 'arenas', 'team',
        ],
        'admin' => [
            'dashboard', 'bug_reports', 'players',
            'notifications', 'seasons', 'servers', 'arenas', 'team',
        ],
        'moderator' => [
            'dashboard', 'bug_reports', 'players',
        ],
    ];

    public static function can(string $role, string $section): bool
    {
        return in_array($section, self::PERMISSIONS[$role] ?? [], true);
    }

    public static function getSections(string $role): array
    {
        return self::PERMISSIONS[$role] ?? [];
    }

    public static function requirePermission(string $section): void
    {
        $admin = AdminAuth::getAdmin();
        if (!$admin || !self::can($admin['role'], $section)) {
            http_response_code(403);
            exit('Access denied');
        }
    }
}
