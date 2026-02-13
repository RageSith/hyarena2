<?php

namespace App\Repository;

use App\Database;

class WebhookRepository
{
    public function getAll(): array
    {
        $db = Database::getConnection();
        $webhooks = $db->query('SELECT * FROM discord_webhooks ORDER BY name')->fetchAll();

        foreach ($webhooks as &$webhook) {
            $stmt = $db->prepare('SELECT event_type FROM webhook_subscriptions WHERE webhook_id = :id');
            $stmt->execute(['id' => $webhook['id']]);
            $webhook['subscriptions'] = $stmt->fetchAll(\PDO::FETCH_COLUMN);
        }

        return $webhooks;
    }

    public function findById(int $id): ?array
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT * FROM discord_webhooks WHERE id = :id');
        $stmt->execute(['id' => $id]);
        $webhook = $stmt->fetch();
        if (!$webhook) return null;

        $stmt = $db->prepare('SELECT event_type FROM webhook_subscriptions WHERE webhook_id = :id');
        $stmt->execute(['id' => $id]);
        $webhook['subscriptions'] = $stmt->fetchAll(\PDO::FETCH_COLUMN);

        return $webhook;
    }

    public function create(array $data): int
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO discord_webhooks (name, url, is_active)
            VALUES (:name, :url, :is_active)
        ');
        $stmt->execute([
            'name' => $data['name'],
            'url' => $data['url'],
            'is_active' => $data['is_active'] ?? 1,
        ]);
        $id = (int) $db->lastInsertId();

        $this->syncSubscriptions($id, $data['subscriptions'] ?? []);
        return $id;
    }

    public function update(int $id, array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            UPDATE discord_webhooks SET name = :name, url = :url, is_active = :is_active WHERE id = :id
        ');
        $stmt->execute([
            'id' => $id,
            'name' => $data['name'],
            'url' => $data['url'],
            'is_active' => $data['is_active'] ?? 1,
        ]);

        $this->syncSubscriptions($id, $data['subscriptions'] ?? []);
    }

    public function delete(int $id): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('DELETE FROM discord_webhooks WHERE id = :id');
        $stmt->execute(['id' => $id]);
    }

    private function syncSubscriptions(int $webhookId, array $eventTypes): void
    {
        $db = Database::getConnection();
        $db->prepare('DELETE FROM webhook_subscriptions WHERE webhook_id = :id')->execute(['id' => $webhookId]);

        $stmt = $db->prepare('INSERT INTO webhook_subscriptions (webhook_id, event_type) VALUES (:id, :event)');
        foreach ($eventTypes as $event) {
            $stmt->execute(['id' => $webhookId, 'event' => $event]);
        }
    }
}
