<?php

namespace App\Service;

use App\Repository\SeasonRepository;

class SeasonService
{
    private SeasonRepository $repo;

    public function __construct()
    {
        $this->repo = new SeasonRepository();
    }

    // ==========================================
    // Match Processing
    // ==========================================

    private function debugLog(string $message): void
    {
        $logFile = __DIR__ . '/../../logs/season_debug.log';
        @mkdir(dirname($logFile), 0775, true);
        file_put_contents($logFile, date('[Y-m-d H:i:s] ') . $message . "\n", FILE_APPEND);
    }

    public function processMatch(
        int $matchId,
        string $arenaId,
        string $gameMode,
        array $participants,
        ?string $winnerUuid,
        int $durationSeconds
    ): void {
        $this->debugLog("processMatch called: match=$matchId arena=$arenaId mode=$gameMode");

        $seasons = $this->repo->getActiveSeasonsForMatch($arenaId, $gameMode);
        $this->debugLog("Found " . count($seasons) . " matching season(s)");

        if (empty($seasons)) {
            return;
        }

        $isWave = $gameMode === 'wave_defense';
        $isSpeedRun = $gameMode === 'speed_run';
        $noWinLoss = $isWave || $isSpeedRun;

        foreach ($seasons as $season) {
            $seasonId = (int) $season['id'];
            $rankingConfig = $season['ranking_config'] ? json_decode($season['ranking_config'], true) : [];

            // Link match to season
            $this->repo->addSeasonMatch($seasonId, $matchId);

            foreach ($participants as $p) {
                $isBot = $p['is_bot'] ?? false;
                if ($isBot || empty($p['uuid'])) {
                    continue;
                }

                $playerUuid = $p['uuid'];

                // Enrollment logic
                if ($season['type'] === 'system') {
                    $this->repo->addParticipant($seasonId, $playerUuid);
                } else {
                    // Private season: skip if not already enrolled
                    if (!$this->repo->isParticipant($seasonId, $playerUuid)) {
                        continue;
                    }
                }

                $isWinner = $playerUuid === $winnerUuid;
                $rankingPoints = $this->calculateRankingPoints($rankingConfig, $p, $isWinner);

                // Extract best_time_ms for speedrun
                $bestTimeMs = null;
                if ($isSpeedRun && !empty($p['json_data'])) {
                    $json = json_decode($p['json_data'], true);
                    if ($json && !($json['is_dnf'] ?? true)) {
                        $bestTimeMs = (int) round(($json['finish_time_nanos'] ?? 0) / 1_000_000);
                    }
                }

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
                    'time_played' => $durationSeconds,
                    'waves_survived' => $p['waves_survived'] ?? null,
                    'best_time_ms' => $bestTimeMs,
                    'ranking_points' => $rankingPoints,
                ];

                $this->repo->updateSeasonStats($seasonId, $playerUuid, $arenaId, $statsData);
            }
        }
    }

    public function calculateRankingPoints(array $config, array $participant, bool $isWinner): float
    {
        $ppw = (float) ($config['points_per_win'] ?? 0);
        $ppl = (float) ($config['points_per_loss'] ?? 0);
        $ppk = (float) ($config['points_per_kill'] ?? 0);
        $ppd = (float) ($config['points_per_death'] ?? 0);

        $kills = ($participant['pvp_kills'] ?? 0) + ($participant['pve_kills'] ?? 0);
        $deaths = ($participant['pvp_deaths'] ?? 0) + ($participant['pve_deaths'] ?? 0);

        return ($isWinner ? $ppw : $ppl) + ($kills * $ppk) + ($deaths * $ppd);
    }

    // ==========================================
    // Season Lifecycle
    // ==========================================

    public function createSeason(array $data): int
    {
        return $this->repo->create($data);
    }

    public function updateSeason(int $id, array $data): void
    {
        $this->repo->update($id, $data);
    }

    public function activateSeason(int $id): void
    {
        $this->repo->updateStatus($id, 'active');
    }

    public function endSeason(int $id): void
    {
        $season = $this->repo->findById($id);
        if (!$season) {
            return;
        }

        $this->repo->freezeRankings($id, $season['ranking_mode']);
        $this->repo->updateStatus($id, 'ended');
    }

    public function archiveSeason(int $id): void
    {
        $this->repo->updateStatus($id, 'archived');
    }

    public function deleteSeason(int $id): void
    {
        $this->repo->delete($id);
    }

    // ==========================================
    // Recurring Season Lifecycle
    // ==========================================

    public function runCronCycle(): array
    {
        $log = [];
        $counts = ['checked_drafts' => 0, 'checked_active' => 0, 'checked_recurring' => 0,
                    'activated' => 0, 'ended' => 0, 'spawned' => 0, 'stopped' => 0];

        // 1. Auto-activate draft seasons past starts_at
        $drafts = $this->repo->getDraftSeasonsToActivate();
        $counts['checked_drafts'] = count($drafts);
        foreach ($drafts as $season) {
            $this->repo->updateStatus((int) $season['id'], 'active');
            $counts['activated']++;
            $log[] = "Activated season #{$season['id']} \"{$season['name']}\"";
        }

        // 2. Auto-end active seasons past ends_at (freeze rankings)
        $actives = $this->repo->getActiveSeasonsToEnd();
        $counts['checked_active'] = count($actives);
        foreach ($actives as $season) {
            $this->endSeason((int) $season['id']);
            $counts['ended']++;
            $log[] = "Ended season #{$season['id']} \"{$season['name']}\"";
        }

        // 3. Spawn next iterations for ended recurring seasons
        $recurring = $this->repo->getEndedRecurringSeasons();
        $counts['checked_recurring'] = count($recurring);
        foreach ($recurring as $season) {
            // Check if recurrence chain has expired
            if (!empty($season['recurrence_ends_at'])) {
                $chainEnd = new \DateTimeImmutable($season['recurrence_ends_at']);
                $nextStart = new \DateTimeImmutable($season['ends_at']);
                if ($nextStart >= $chainEnd) {
                    $this->repo->updateRecurrence((int) $season['id'], 'none');
                    $counts['stopped']++;
                    $log[] = "Stopped recurring season #{$season['id']} \"{$season['name']}\" (recurrence_ends_at reached)";
                    continue;
                }
            }

            $newId = $this->spawnNextIteration($season);
            $counts['spawned']++;
            $log[] = "Spawned iteration #{$newId} from season #{$season['id']} \"{$season['name']}\" (recurrence={$season['recurrence']})";
        }

        return ['actions' => $log, 'counts' => $counts];
    }

    public function spawnNextIteration(array $endedSeason): int
    {
        $recurrence = $endedSeason['recurrence'];
        $baseName = $endedSeason['base_name'] ?? $endedSeason['name'];

        $endsAt = new \DateTimeImmutable($endedSeason['ends_at']);
        $nextStartsAt = $endsAt;

        switch ($recurrence) {
            case 'daily':
                $nextEndsAt = $nextStartsAt->modify('+1 day');
                break;
            case 'weekly':
                $nextEndsAt = $nextStartsAt->modify('+7 days');
                break;
            case 'monthly':
                $nextEndsAt = $nextStartsAt->modify('+1 month');
                break;
            case 'yearly':
                $nextEndsAt = $nextStartsAt->modify('+1 year');
                break;
            default:
                $origStart = new \DateTimeImmutable($endedSeason['starts_at']);
                $origEnd = new \DateTimeImmutable($endedSeason['ends_at']);
                $duration = $origStart->diff($origEnd);
                $nextEndsAt = $nextStartsAt->add($duration);
                break;
        }

        // Increment iteration number
        $nextIteration = ((int) ($endedSeason['iteration'] ?? 0)) + 1;
        $baseSlug = preg_replace('/-\d+$/', '', $endedSeason['slug']);

        $data = [
            'name' => $baseName . ' #' . $nextIteration,
            'slug' => $baseSlug . '-' . $nextIteration,
            'description' => $endedSeason['description'],
            'type' => $endedSeason['type'],
            'status' => 'active',
            'starts_at' => $nextStartsAt->format('Y-m-d H:i:s'),
            'ends_at' => $nextEndsAt->format('Y-m-d H:i:s'),
            'ranking_mode' => $endedSeason['ranking_mode'],
            'ranking_config' => $endedSeason['ranking_config'] ? json_decode($endedSeason['ranking_config'], true) : null,
            'min_matches' => (int) $endedSeason['min_matches'],
            'arena_ids' => $endedSeason['arena_ids'] ? json_decode($endedSeason['arena_ids'], true) : null,
            'game_mode_ids' => $endedSeason['game_mode_ids'] ? json_decode($endedSeason['game_mode_ids'], true) : null,
            'visibility' => $endedSeason['visibility'],
            'join_code' => $endedSeason['join_code'],
            'recurrence' => $recurrence,
            'recurrence_ends_at' => $endedSeason['recurrence_ends_at'],
            'base_name' => $baseName,
            'iteration' => $nextIteration,
            'parent_season_id' => (int) $endedSeason['id'],
        ];

        $newId = $this->repo->create($data);

        // Auto-archive the old iteration
        $this->repo->updateStatus((int) $endedSeason['id'], 'archived');

        return $newId;
    }

    // ==========================================
    // Query Passthroughs
    // ==========================================

    public function getPublicActiveSeasons(): array
    {
        return $this->repo->getPublicActive();
    }

    public function getActiveSeasonsForPlayer(?string $playerUuid): array
    {
        return $this->repo->getActiveForPlayer($playerUuid);
    }

    public function getEndedSeasons(int $limit = 25, int $offset = 0): array
    {
        return $this->repo->getPublicEnded($limit, $offset);
    }

    public function getEndedSeasonsCount(): int
    {
        return $this->repo->getPublicEndedCount();
    }

    public function getAllSeasons(): array
    {
        return $this->repo->getAll();
    }

    public function getSeasonById(int $id): ?array
    {
        return $this->repo->findById($id);
    }

    public function getSeasonBySlug(string $slug): ?array
    {
        $season = $this->repo->findBySlug($slug);
        if ($season) {
            $season['participant_count'] = $this->repo->getParticipantCount((int) $season['id']);
        }
        return $season;
    }

    public function getSeasonLeaderboard(int $seasonId, string $rankingMode, string $order = 'DESC', int $limit = 25, int $offset = 0, int $minMatches = 0): array
    {
        return $this->repo->getSeasonLeaderboard($seasonId, $rankingMode, $order, $limit, $offset, $minMatches);
    }

    public function getSeasonLeaderboardCount(int $seasonId, int $minMatches = 0): int
    {
        return $this->repo->getSeasonLeaderboardCount($seasonId, $minMatches);
    }

    public function getFrozenRankings(int $seasonId, int $limit = 25, int $offset = 0): array
    {
        return $this->repo->getFrozenRankings($seasonId, $limit, $offset);
    }

    public function getFrozenRankingsCount(int $seasonId): int
    {
        return $this->repo->getFrozenRankingsCount($seasonId);
    }

    public function getPlayerSeasonHistory(string $playerUuid): array
    {
        return $this->repo->getPlayerSeasonHistory($playerUuid);
    }

    // ==========================================
    // Player Season Management
    // ==========================================

    public function joinByCode(string $playerUuid, string $code): array
    {
        $season = $this->repo->findByJoinCode($code);
        if (!$season) {
            return ['success' => false, 'error' => 'Invalid or expired season code.'];
        }

        if ($season['type'] !== 'private') {
            return ['success' => false, 'error' => 'This code is not for a private season.'];
        }

        $seasonId = (int) $season['id'];

        if ($this->repo->isOptedOut($seasonId, $playerUuid)) {
            return ['success' => false, 'error' => 'You have already left this season.'];
        }

        if ($this->repo->isParticipant($seasonId, $playerUuid)) {
            return ['success' => false, 'error' => 'You are already in this season.'];
        }

        $this->repo->addParticipant($seasonId, $playerUuid);
        return ['success' => true, 'season_name' => $season['name']];
    }

    public function optOut(string $playerUuid, int $seasonId): array
    {
        $season = $this->repo->findById($seasonId);
        if (!$season) {
            return ['success' => false, 'error' => 'Season not found.'];
        }

        if ($season['status'] !== 'active') {
            return ['success' => false, 'error' => 'This season is no longer active.'];
        }

        if ($season['type'] !== 'private') {
            return ['success' => false, 'error' => 'You can only leave private seasons.'];
        }

        if ($this->repo->isOptedOut($seasonId, $playerUuid)) {
            return ['success' => false, 'error' => 'You have already left this season.'];
        }

        if (!$this->repo->isParticipant($seasonId, $playerUuid)) {
            return ['success' => false, 'error' => 'You are not in this season.'];
        }

        $this->repo->optOut($seasonId, $playerUuid);
        return ['success' => true, 'season_name' => $season['name']];
    }

    public function getPlayerPrivateSeasons(string $playerUuid): array
    {
        return $this->repo->getPlayerPrivateSeasons($playerUuid);
    }
}
