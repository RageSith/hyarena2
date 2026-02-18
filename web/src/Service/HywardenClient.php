<?php

namespace App\Service;

use App\Config;

class HywardenClient
{
    private string $baseUrl;
    private string $username;
    private string $password;
    private string $tokenFile;
    private ?string $token = null;
    private int $tokenExp = 0;

    public function __construct()
    {
        $cfg = Config::get('hywarden', []);
        $this->baseUrl = rtrim($cfg['url'] ?? 'http://localhost:8080', '/');
        $this->username = $cfg['username'] ?? '';
        $this->password = $cfg['password'] ?? '';
        $this->tokenFile = $cfg['token_file'] ?? '/tmp/hywarden_token.json';
        $this->loadCachedToken();
    }

    public function getServers(): array
    {
        return $this->request('GET', '/api/server-configs');
    }

    public function getMetrics(): array
    {
        return $this->request('GET', '/api/system/metrics');
    }

    public function getConsole(string $id, int $offset = -1, int $limit = 500): array
    {
        $query = '?limit=' . $limit;
        if ($offset >= 0) {
            $query .= '&offset=' . $offset;
        }
        return $this->request('GET', '/api/server-configs/' . urlencode($id) . '/console' . $query);
    }

    public function startServer(string $id): array
    {
        return $this->request('POST', '/api/server-configs/' . urlencode($id) . '/start');
    }

    public function stopServer(string $id): array
    {
        return $this->request('POST', '/api/server-configs/' . urlencode($id) . '/stop');
    }

    public function killServer(string $id): array
    {
        return $this->request('POST', '/api/server-configs/' . urlencode($id) . '/kill');
    }

    // ==========================================
    // HTTP + Token Lifecycle
    // ==========================================

    private function request(string $method, string $path, bool $retry = true): array
    {
        $this->ensureToken();

        $url = $this->baseUrl . $path;
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 10,
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_HTTPHEADER => [
                'Authorization: Bearer ' . $this->token,
                'Content-Type: application/json',
            ],
        ]);

        $body = curl_exec($ch);
        $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);

        if ($error) {
            return ['error' => 'Connection failed: ' . $error];
        }

        if ($status === 401 && $retry) {
            $this->login();
            return $this->request($method, $path, false);
        }

        $data = json_decode($body, true);
        if ($data === null && $body !== 'null') {
            return ['error' => 'Invalid JSON response (HTTP ' . $status . ')'];
        }

        if ($status >= 400) {
            return ['error' => $data['error'] ?? 'HTTP ' . $status];
        }

        return $data ?? [];
    }

    private function ensureToken(): void
    {
        if ($this->token && $this->tokenExp > time() + 60) {
            return;
        }
        $this->login();
    }

    private function login(): void
    {
        $url = $this->baseUrl . '/api/auth/login';
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 10,
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => json_encode([
                'username' => $this->username,
                'password' => $this->password,
            ]),
            CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
        ]);

        $body = curl_exec($ch);
        $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($status !== 200) {
            $this->token = null;
            $this->tokenExp = 0;
            return;
        }

        $data = json_decode($body, true);
        $token = $data['token'] ?? null;
        if (!$token) {
            return;
        }

        $this->token = $token;
        $this->tokenExp = $this->parseJwtExp($token);
        $this->saveCachedToken();
    }

    private function parseJwtExp(string $token): int
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            return 0;
        }
        $payload = json_decode(base64_decode(strtr($parts[1], '-_', '+/')), true);
        return $payload['exp'] ?? 0;
    }

    private function loadCachedToken(): void
    {
        if (!file_exists($this->tokenFile)) {
            return;
        }
        $data = json_decode(file_get_contents($this->tokenFile), true);
        if (!$data || empty($data['token']) || empty($data['exp'])) {
            return;
        }
        if ($data['exp'] <= time() + 60) {
            return;
        }
        $this->token = $data['token'];
        $this->tokenExp = $data['exp'];
    }

    private function saveCachedToken(): void
    {
        file_put_contents($this->tokenFile, json_encode([
            'token' => $this->token,
            'exp' => $this->tokenExp,
        ]), LOCK_EX);
    }
}
