<?php

namespace App\Controller;

use App\Repository\PlayerRepository;
use App\Repository\MatchRepository;
use App\Repository\StatsRepository;
use App\Repository\ArenaRepository;
use App\Repository\KitRepository;
use App\Service\MatchSubmissionService;
use App\Service\SyncService;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class ApiController
{
    private function json(Response $response, array $data, int $status = 200): Response
    {
        $response->getBody()->write(json_encode($data, JSON_UNESCAPED_UNICODE));
        return $response
            ->withHeader('Content-Type', 'application/json')
            ->withStatus($status);
    }

    private function success(Response $response, mixed $data = null): Response
    {
        return $this->json($response, ['success' => true, 'data' => $data]);
    }

    private function error(Response $response, string $message, string $code, int $status = 400): Response
    {
        return $this->json($response, [
            'success' => false,
            'error' => ['message' => $message, 'code' => $code],
        ], $status);
    }

    public function submitMatch(Request $request, Response $response): Response
    {
        $data = $request->getParsedBody();
        if (!$data) {
            $data = json_decode((string) $request->getBody(), true);
        }

        if (empty($data['arena_id']) || empty($data['game_mode']) || empty($data['participants'])) {
            return $this->error($response, 'Missing required fields: arena_id, game_mode, participants', 'VALIDATION_ERROR');
        }

        try {
            $service = new MatchSubmissionService();
            $result = $service->submit($data);
            return $this->success($response, $result);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to submit match: ' . $e->getMessage(), 'SUBMISSION_ERROR', 500);
        }
    }

    public function sync(Request $request, Response $response): Response
    {
        $data = $request->getParsedBody();
        if (!$data) {
            $data = json_decode((string) $request->getBody(), true);
        }

        if (!$data || (!isset($data['arenas']) && !isset($data['kits']))) {
            return $this->error($response, 'No data to sync', 'VALIDATION_ERROR');
        }

        try {
            $service = new SyncService();
            $result = $service->sync($data);
            return $this->success($response, $result);
        } catch (\Exception $e) {
            return $this->error($response, 'Sync failed: ' . $e->getMessage(), 'SYNC_ERROR', 500);
        }
    }

    public function serverStats(Request $request, Response $response): Response
    {
        try {
            $playerRepo = new PlayerRepository();
            $matchRepo = new MatchRepository();
            $arenaRepo = new ArenaRepository();
            $kitRepo = new KitRepository();

            return $this->success($response, [
                'total_players' => $playerRepo->getTotalCount(),
                'total_matches' => $matchRepo->getTotalCount(),
                'total_arenas' => $arenaRepo->getCount(),
                'total_kits' => $kitRepo->getCount(),
            ]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch stats', 'SERVER_ERROR', 500);
        }
    }

    public function leaderboard(Request $request, Response $response): Response
    {
        $params = $request->getQueryParams();
        $arena = $params['arena'] ?? null;
        $sort = $params['sort'] ?? 'pvp_kills';
        $order = $params['order'] ?? 'DESC';
        $page = max(1, (int) ($params['page'] ?? 1));
        $perPage = min(50, max(1, (int) ($params['per_page'] ?? 25)));
        $offset = ($page - 1) * $perPage;

        // Treat "global" as null (global stats)
        if ($arena === 'global') {
            $arena = null;
        }

        try {
            $statsRepo = new StatsRepository();
            $entries = $statsRepo->getLeaderboard($sort, $order, $perPage, $offset, $arena);
            $total = $statsRepo->getLeaderboardCount($arena);

            return $this->success($response, [
                'entries' => $entries,
                'total' => $total,
                'page' => $page,
                'per_page' => $perPage,
                'total_pages' => (int) ceil($total / $perPage),
            ]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch leaderboard', 'SERVER_ERROR', 500);
        }
    }

    public function player(Request $request, Response $response, array $args): Response
    {
        $identifier = $args['identifier'];

        try {
            $playerRepo = new PlayerRepository();
            $statsRepo = new StatsRepository();

            // Try UUID first (36 chars with dashes), then username
            $player = strlen($identifier) === 36
                ? $playerRepo->findByUuid($identifier)
                : $playerRepo->findByUsername($identifier);

            if (!$player) {
                return $this->error($response, 'Player not found', 'NOT_FOUND', 404);
            }

            $globalStats = $statsRepo->getGlobalStats($player['uuid']);
            $arenaStats = $statsRepo->getArenaStats($player['uuid']);
            $recentMatches = $statsRepo->getPlayerRecentMatches($player['uuid']);

            return $this->success($response, [
                'player' => $player,
                'global_stats' => $globalStats,
                'arena_stats' => $arenaStats,
                'recent_matches' => $recentMatches,
            ]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch player data', 'SERVER_ERROR', 500);
        }
    }

    public function recentMatches(Request $request, Response $response): Response
    {
        $params = $request->getQueryParams();
        $limit = min(50, max(1, (int) ($params['limit'] ?? 10)));
        $offset = max(0, (int) ($params['offset'] ?? 0));

        try {
            $matchRepo = new MatchRepository();
            $matches = $matchRepo->getRecentMatches($limit, $offset);
            return $this->success($response, $matches);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch matches', 'SERVER_ERROR', 500);
        }
    }

    public function arenas(Request $request, Response $response): Response
    {
        try {
            $arenaRepo = new ArenaRepository();
            return $this->success($response, $arenaRepo->getAll());
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch arenas', 'SERVER_ERROR', 500);
        }
    }

    public function kits(Request $request, Response $response): Response
    {
        try {
            $kitRepo = new KitRepository();
            return $this->success($response, $kitRepo->getAll());
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch kits', 'SERVER_ERROR', 500);
        }
    }

    public function playerSync(Request $request, Response $response): Response
    {
        $data = $request->getParsedBody();
        if (!$data) {
            $data = json_decode((string) $request->getBody(), true);
        }

        if (empty($data['players']) || !is_array($data['players'])) {
            return $this->error($response, 'Missing required field: players (array)', 'VALIDATION_ERROR');
        }

        try {
            $playerRepo = new PlayerRepository();
            $updated = $playerRepo->batchUpdateEconomy($data['players']);
            return $this->success($response, ['updated' => $updated]);
        } catch (\Exception $e) {
            return $this->error($response, 'Player sync failed: ' . $e->getMessage(), 'SYNC_ERROR', 500);
        }
    }
}
