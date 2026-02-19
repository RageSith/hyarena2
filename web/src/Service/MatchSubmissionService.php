<?php

namespace App\Service;

use App\Database;
use App\Repository\PlayerRepository;
use App\Repository\MatchRepository;
use App\Repository\ParticipantRepository;
use App\Repository\StatsRepository;
use App\Repository\KitRepository;
use App\Service\SeasonService;

class MatchSubmissionService
{
    private PlayerRepository $playerRepo;
    private MatchRepository $matchRepo;
    private ParticipantRepository $participantRepo;
    private StatsRepository $statsRepo;
    private KitRepository $kitRepo;
    private SeasonService $seasonService;

    public function __construct()
    {
        $this->playerRepo = new PlayerRepository();
        $this->matchRepo = new MatchRepository();
        $this->participantRepo = new ParticipantRepository();
        $this->statsRepo = new StatsRepository();
        $this->kitRepo = new KitRepository();
        $this->seasonService = new SeasonService();
    }

    public function submit(array $data): array
    {
        $db = Database::getConnection();
        $db->beginTransaction();

        try {
            // Upsert all players
            foreach ($data['participants'] as $p) {
                if (!($p['is_bot'] ?? false) && !empty($p['uuid'])) {
                    $this->playerRepo->upsert($p['uuid'], $p['username']);
                }
            }

            // Create match
            $matchId = $this->matchRepo->create(
                $data['arena_id'],
                $data['game_mode'],
                $data['winner_uuid'] ?? null,
                $data['duration_seconds'] ?? 0
            );

            // Insert participants and update stats
            foreach ($data['participants'] as $p) {
                $isBot = $p['is_bot'] ?? false;
                $isWinner = $p['is_winner'] ?? false;

                // Extract finish_time_ms for speedrun participants
                $finishTimeMs = null;
                if (($data['game_mode'] ?? '') === 'speed_run' && !empty($p['json_data'])) {
                    $jsonData = json_decode($p['json_data'], true);
                    if ($jsonData && !($jsonData['is_dnf'] ?? true)) {
                        $finishTimeMs = (int) round(($jsonData['finish_time_nanos'] ?? 0) / 1_000_000);
                    }
                }

                $this->participantRepo->create([
                    'match_id' => $matchId,
                    'player_uuid' => $isBot ? null : ($p['uuid'] ?? null),
                    'is_bot' => $isBot,
                    'bot_name' => $p['bot_name'] ?? null,
                    'bot_difficulty' => $p['bot_difficulty'] ?? null,
                    'kit_id' => $p['kit_id'] ?? null,
                    'pvp_kills' => $p['pvp_kills'] ?? 0,
                    'pvp_deaths' => $p['pvp_deaths'] ?? 0,
                    'pve_kills' => $p['pve_kills'] ?? 0,
                    'pve_deaths' => $p['pve_deaths'] ?? 0,
                    'damage_dealt' => $p['damage_dealt'] ?? 0,
                    'damage_taken' => $p['damage_taken'] ?? 0,
                    'is_winner' => $isWinner,
                    'waves_survived' => $p['waves_survived'] ?? null,
                    'json_data' => $p['json_data'] ?? null,
                    'finish_time_ms' => $finishTimeMs,
                ]);

                // Update player stats (skip bots)
                if (!$isBot && !empty($p['uuid'])) {
                    $isWave = ($data['game_mode'] ?? '') === 'wave_defense';
                    $isSpeedRun = ($data['game_mode'] ?? '') === 'speed_run';
                    $noWinLoss = $isWave || $isSpeedRun;
                    $statsData = [
                        'matches_played' => 1,
                        'matches_won' => $noWinLoss ? 0 : ($isWinner ? 1 : 0),
                        'matches_lost' => $noWinLoss ? 0 : ($isWinner ? 0 : 1),
                        'pvp_kills' => $p['pvp_kills'] ?? 0,
                        'pvp_deaths' => $p['pvp_deaths'] ?? 0,
                        'pve_kills' => $p['pve_kills'] ?? 0,
                        'pve_deaths' => $p['pve_deaths'] ?? 0,
                        'damage_dealt' => $p['damage_dealt'] ?? 0,
                        'damage_taken' => $p['damage_taken'] ?? 0,
                        'time_played' => $data['duration_seconds'] ?? 0,
                        'waves_survived' => $p['waves_survived'] ?? null,
                        'best_time_ms' => $finishTimeMs,
                    ];

                    // Update per-arena stats (global stats derived from view)
                    $this->statsRepo->updateStats($p['uuid'], $data['arena_id'], $statsData);

                    // Update per-kit stats
                    $kitId = $p['kit_id'] ?? null;
                    if ($kitId !== null) {
                        $this->kitRepo->ensureExists($kitId);
                        $this->statsRepo->updateKitStats($p['uuid'], $kitId, $statsData);
                    }

                    // Update economy snapshot if provided
                    if (isset($p['arena_points'])) {
                        $this->playerRepo->updateEconomy(
                            $p['uuid'],
                            (int) $p['arena_points'],
                            (int) ($p['honor'] ?? 0),
                            $p['honor_rank'] ?? 'Unranked'
                        );
                    }
                }
            }

            // Process season stats (fail-safe: log but don't rollback)
            try {
                $this->seasonService->processMatch(
                    $matchId,
                    $data['arena_id'],
                    $data['game_mode'],
                    $data['participants'],
                    $data['winner_uuid'] ?? null,
                    $data['duration_seconds'] ?? 0
                );
            } catch (\Exception $seasonEx) {
                $logFile = __DIR__ . '/../../logs/season_debug.log';
                @mkdir(dirname($logFile), 0775, true);
                file_put_contents($logFile, date('[Y-m-d H:i:s] ') . 'ERROR match=' . $matchId . ': ' . $seasonEx->getMessage() . "\n" . $seasonEx->getTraceAsString() . "\n\n", FILE_APPEND);
            }

            $db->commit();
            return ['match_id' => $matchId];
        } catch (\Exception $e) {
            $db->rollBack();
            throw $e;
        }
    }
}
