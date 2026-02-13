<?php

namespace App\Service;

use App\Repository\ArenaRepository;
use App\Repository\KitRepository;

class SyncService
{
    private ArenaRepository $arenaRepo;
    private KitRepository $kitRepo;

    public function __construct()
    {
        $this->arenaRepo = new ArenaRepository();
        $this->kitRepo = new KitRepository();
    }

    public function sync(array $data): array
    {
        $synced = ['arenas' => 0, 'kits' => 0];

        if (!empty($data['arenas'])) {
            foreach ($data['arenas'] as $arena) {
                $this->arenaRepo->upsert($arena);
                $synced['arenas']++;
            }
        }

        if (!empty($data['kits'])) {
            foreach ($data['kits'] as $kit) {
                $this->kitRepo->upsert($kit);
                $synced['kits']++;
            }
        }

        return $synced;
    }
}
