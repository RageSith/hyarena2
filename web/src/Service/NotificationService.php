<?php

namespace App\Service;

use App\Repository\NotificationRepository;
use Parsedown;

class NotificationService
{
    private NotificationRepository $repo;
    private Parsedown $parsedown;

    public function __construct()
    {
        $this->repo = new NotificationRepository();
        $this->parsedown = new Parsedown();
        $this->parsedown->setSafeMode(true);
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

    public function getActive(): array
    {
        return $this->repo->getActive();
    }

    public function getActivePaginated(int $limit, int $offset): array
    {
        return $this->repo->getActivePaginated($limit, $offset);
    }

    public function getActiveCount(): int
    {
        return $this->repo->getActiveCount();
    }

    public function getLatestActive(int $limit = 3): array
    {
        return $this->repo->getLatestActive($limit);
    }

    public function renderMarkdown(string $text): string
    {
        return $this->parsedown->text($text);
    }
}
