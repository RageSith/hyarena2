<?php

namespace App\Service;

use App\Database;
use App\Repository\PlayerRepository;
use App\Repository\MatchRepository;
use App\Repository\ParticipantRepository;
use App\Repository\StatsRepository;

class MatchSubmissionService
{
    private PlayerRepository $playerRepo;
    private MatchRepository $matchRepo;
    private ParticipantRepository $participantRepo;
    private StatsRepository $statsRepo;

    public function __construct()
    {
        $this->playerRepo = new PlayerRepository();
        $this->matchRepo = new MatchRepository();
        $this->participantRepo = new ParticipantRepository();
        $this->statsRepo = new StatsRepository();
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
                $isWinner = ($p['uuid'] ?? null) === ($data['winner_uuid'] ?? '__none__');

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
                ]);

                // Update player stats (skip bots)
                if (!$isBot && !empty($p['uuid'])) {
                    $statsData = [
                        'matches_played' => 1,
                        'matches_won' => $isWinner ? 1 : 0,
                        'matches_lost' => $isWinner ? 0 : 1,
                        'pvp_kills' => $p['pvp_kills'] ?? 0,
                        'pvp_deaths' => $p['pvp_deaths'] ?? 0,
                        'pve_kills' => $p['pve_kills'] ?? 0,
                        'pve_deaths' => $p['pve_deaths'] ?? 0,
                        'damage_dealt' => $p['damage_dealt'] ?? 0,
                        'damage_taken' => $p['damage_taken'] ?? 0,
                        'time_played' => $data['duration_seconds'] ?? 0,
                    ];

                    // Update per-arena stats
                    $this->statsRepo->updateStats($p['uuid'], $data['arena_id'], $statsData);
                    // Update global stats
                    $this->statsRepo->updateStats($p['uuid'], null, $statsData);
                }
            }

            $db->commit();
            return ['match_id' => $matchId];
        } catch (\Exception $e) {
            $db->rollBack();
            throw $e;
        }
    }
}
