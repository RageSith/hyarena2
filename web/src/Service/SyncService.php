<?php

namespace App\Service;

use App\Repository\ArenaRepository;
use App\Repository\KitRepository;
use App\Repository\GameModeRepository;

class SyncService
{
    private ArenaRepository $arenaRepo;
    private KitRepository $kitRepo;
    private GameModeRepository $gameModeRepo;

    public function __construct()
    {
        $this->arenaRepo = new ArenaRepository();
        $this->kitRepo = new KitRepository();
        $this->gameModeRepo = new GameModeRepository();
    }

    public function sync(array $data): array
    {
        $synced = ['arenas' => 0, 'kits' => 0, 'game_modes' => 0];

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

        if (!empty($data['game_modes'])) {
            foreach ($data['game_modes'] as $gameMode) {
                $this->gameModeRepo->upsert($gameMode);
                $synced['game_modes']++;
            }
        }

        return $synced;
    }
}
