<?php

namespace App\Service;

use App\Database;
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
        $db = Database::getConnection();
        $db->beginTransaction();

        try {
            $synced = ['arenas' => 0, 'kits' => 0, 'game_modes' => 0];

            if (!empty($data['arenas'])) {
                $this->arenaRepo->markAllInvisible();
                foreach ($data['arenas'] as $arena) {
                    $this->arenaRepo->upsert($arena);
                    $synced['arenas']++;
                }
            }

            if (!empty($data['kits'])) {
                $this->kitRepo->markAllInvisible();
                foreach ($data['kits'] as $kit) {
                    $this->kitRepo->upsert($kit);
                    $synced['kits']++;
                }
            }

            if (!empty($data['game_modes'])) {
                $this->gameModeRepo->markAllInvisible();
                foreach ($data['game_modes'] as $gameMode) {
                    $this->gameModeRepo->upsert($gameMode);
                    $synced['game_modes']++;
                }
            }

            $db->commit();
            return $synced;
        } catch (\Exception $e) {
            $db->rollBack();
            throw $e;
        }
    }
}
