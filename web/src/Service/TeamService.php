<?php

namespace App\Service;

use App\Repository\TeamRepository;

class TeamService
{
    private TeamRepository $repo;

    public function __construct()
    {
        $this->repo = new TeamRepository();
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

    public function getMaxSortOrder(): int
    {
        return $this->repo->getMaxSortOrder();
    }

    public function moveUp(int $id): void
    {
        $member = $this->repo->findById($id);
        if (!$member) return;

        $adjacent = $this->repo->getAdjacentMember($member['sort_order'], 'up');
        if ($adjacent) {
            $this->repo->swapSortOrder($member['id'], $adjacent['id']);
        }
    }

    public function moveDown(int $id): void
    {
        $member = $this->repo->findById($id);
        if (!$member) return;

        $adjacent = $this->repo->getAdjacentMember($member['sort_order'], 'down');
        if ($adjacent) {
            $this->repo->swapSortOrder($member['id'], $adjacent['id']);
        }
    }
}
