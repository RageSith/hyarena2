<?php

namespace App\Auth;

use App\Database;
use App\Config;

class AdminAuth
{
    private static string $sessionName = 'hyarena_admin';

    public static function init(): void
    {
        // Already running the admin session — nothing to do
        if (session_status() === PHP_SESSION_ACTIVE && session_name() === self::$sessionName) {
            return;
        }

        // A player session may be active — close it so we can start the admin one
        if (session_status() === PHP_SESSION_ACTIVE) {
            session_write_close();
        }

        $lifetime = Config::get('admin.session_lifetime', 3600);
        session_name(self::$sessionName);
        session_set_cookie_params([
            'lifetime' => $lifetime,
            'path' => '/admin',
            'httponly' => true,
            'samesite' => 'Strict',
        ]);
        session_start();

        // Regenerate session ID periodically
        if (!isset($_SESSION['_last_regen']) || (time() - $_SESSION['_last_regen']) > 1800) {
            session_regenerate_id(true);
            $_SESSION['_last_regen'] = time();
        }
    }

    public static function login(string $username, string $password, string $ip): array
    {
        $db = Database::getConnection();

        // Check rate limiting
        if (self::isRateLimited($ip)) {
            return ['success' => false, 'error' => 'Too many login attempts. Please try again later.'];
        }

        $stmt = $db->prepare('SELECT * FROM admin_users WHERE username = :username');
        $stmt->execute(['username' => $username]);
        $admin = $stmt->fetch();

        // Log attempt
        $success = $admin && password_verify($password, $admin['password_hash']);
        self::logAttempt($ip, $username, $success);

        if (!$success) {
            return ['success' => false, 'error' => 'Invalid username or password.'];
        }

        // Update last login
        $stmt = $db->prepare('UPDATE admin_users SET last_login = NOW() WHERE id = :id');
        $stmt->execute(['id' => $admin['id']]);

        // Set session
        self::init();
        $_SESSION['admin_id'] = $admin['id'];
        $_SESSION['admin_username'] = $admin['username'];
        $_SESSION['admin_role'] = $admin['role'];
        $_SESSION['admin_login_time'] = time();

        return ['success' => true];
    }

    public static function logout(): void
    {
        self::init();
        $_SESSION = [];

        // Expire the admin session cookie
        $params = session_get_cookie_params();
        setcookie(self::$sessionName, '', time() - 3600, $params['path'], $params['domain'], $params['secure'], $params['httponly']);

        session_destroy();
    }

    public static function isLoggedIn(): bool
    {
        self::init();
        return isset($_SESSION['admin_id']);
    }

    public static function getAdmin(): ?array
    {
        if (!self::isLoggedIn()) return null;
        return [
            'id' => $_SESSION['admin_id'],
            'username' => $_SESSION['admin_username'],
            'role' => $_SESSION['admin_role'],
        ];
    }

    public static function generateCsrfToken(): string
    {
        self::init();
        if (empty($_SESSION['csrf_token'])) {
            $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
        }
        return $_SESSION['csrf_token'];
    }

    public static function validateCsrfToken(string $token): bool
    {
        self::init();
        return isset($_SESSION['csrf_token']) && hash_equals($_SESSION['csrf_token'], $token);
    }

    private static function isRateLimited(string $ip): bool
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT COUNT(*) FROM admin_login_attempts
            WHERE ip_address = :ip AND success = 0
            AND attempted_at > DATE_SUB(NOW(), INTERVAL 15 MINUTE)
        ');
        $stmt->execute(['ip' => $ip]);
        return (int) $stmt->fetchColumn() >= 5;
    }

    private static function logAttempt(string $ip, string $username, bool $success): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO admin_login_attempts (ip_address, username, success)
            VALUES (:ip, :username, :success)
        ');
        $stmt->execute(['ip' => $ip, 'username' => $username, 'success' => $success ? 1 : 0]);
    }
}
