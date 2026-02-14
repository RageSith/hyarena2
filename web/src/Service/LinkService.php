<?php

namespace App\Service;

use App\Repository\LinkedAccountRepository;
use App\Repository\PlayerRepository;

class LinkService
{
    private LinkedAccountRepository $repo;

    public function __construct()
    {
        $this->repo = new LinkedAccountRepository();
    }

    public function generateCode(string $playerUuid, string $username): string
    {
        // Ensure player exists in DB (handles FK constraint for new players)
        $playerRepo = new PlayerRepository();
        $playerRepo->upsert($playerUuid, $username);

        // 6-char code, no ambiguous chars (0/O, 1/I/L)
        $chars = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
        $code = '';
        for ($i = 0; $i < 6; $i++) {
            $code .= $chars[random_int(0, strlen($chars) - 1)];
        }

        $expiresAt = date('Y-m-d H:i:s', time() + 600); // 10 minutes
        $this->repo->storeLinkCode($code, $playerUuid, $expiresAt);

        return $code;
    }

    public function register(string $email, string $password): array
    {
        $existing = $this->repo->findByEmail($email);
        if ($existing) {
            return ['success' => false, 'error' => 'An account with this email already exists.'];
        }

        $hash = password_hash($password, PASSWORD_BCRYPT);
        $id = $this->repo->create($email, $hash);

        return ['success' => true, 'account_id' => $id];
    }

    public function login(string $email, string $password): array
    {
        $account = $this->repo->findByEmail($email);
        if (!$account || !password_verify($password, $account['password_hash'])) {
            return ['success' => false, 'error' => 'Invalid email or password.'];
        }

        return ['success' => true, 'account' => $account];
    }

    public function linkAccount(int $accountId, string $code): array
    {
        $linkCode = $this->repo->findValidCode(strtoupper($code));
        if (!$linkCode) {
            return ['success' => false, 'error' => 'Invalid or expired link code.'];
        }

        // Check if this player UUID is already linked
        $existing = $this->repo->findByPlayerUuid($linkCode['player_uuid']);
        if ($existing && $existing['id'] !== $accountId) {
            return ['success' => false, 'error' => 'This game account is already linked to another web account.'];
        }

        $this->repo->linkPlayer($accountId, $linkCode['player_uuid']);
        $this->repo->markCodeUsed($linkCode['id']);

        return ['success' => true, 'player_uuid' => $linkCode['player_uuid']];
    }

    public function getAccount(int $id): ?array
    {
        return $this->repo->findById($id);
    }

    public function changePassword(int $accountId, string $currentPassword, string $newPassword): array
    {
        $account = $this->repo->findById($accountId);
        if (!$account) {
            return ['success' => false, 'error' => 'Account not found.'];
        }

        if (!password_verify($currentPassword, $account['password_hash'])) {
            return ['success' => false, 'error' => 'Current password is incorrect.'];
        }

        $hash = password_hash($newPassword, PASSWORD_BCRYPT);
        $this->repo->updatePassword($accountId, $hash);

        return ['success' => true];
    }
}
