<?php

namespace App\Service;

use App\Repository\WebhookRepository;

class WebhookService
{
    private WebhookRepository $repo;

    public function __construct()
    {
        $this->repo = new WebhookRepository();
    }

    public function getAll(): array
    {
        return $this->repo->getAll();
    }

    public function findById(int $id): ?array
    {
        return $this->repo->findById($id);
    }

    public function create(array $data): int
    {
        return $this->repo->create($data);
    }

    public function update(int $id, array $data): void
    {
        $this->repo->update($id, $data);
    }

    public function delete(int $id): void
    {
        $this->repo->delete($id);
    }

    public function dispatch(string $eventType, array $payload): void
    {
        $webhooks = $this->repo->getAll();

        foreach ($webhooks as $webhook) {
            if (!$webhook['is_active']) continue;
            if (!in_array($eventType, $webhook['subscriptions'])) continue;

            $this->sendToWebhook($webhook['url'], $payload);
        }
    }

    private function sendToWebhook(string $url, array $payload): void
    {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
            CURLOPT_POSTFIELDS => json_encode($payload),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 10,
        ]);
        curl_exec($ch);
        curl_close($ch);
    }
}
