<?php

namespace App\Controller;

use App\Repository\PlayerRepository;
use App\Repository\MatchRepository;
use App\Repository\StatsRepository;
use App\Repository\ArenaRepository;
use App\Repository\KitRepository;
use App\Repository\GameModeRepository;
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
            $statsRepo = new StatsRepository();

            return $this->success($response, [
                'total_players' => $playerRepo->getTotalCount(),
                'total_matches' => $matchRepo->getTotalCount(),
                'total_kills' => $statsRepo->getTotalKills(),
                'matches_today' => $matchRepo->getMatchesToday(),
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
        $gameMode = $params['game_mode'] ?? null;
        $sort = $params['sort'] ?? 'pvp_kills';
        $order = $params['order'] ?? 'DESC';
        $page = max(1, (int) ($params['page'] ?? 1));
        $perPage = min(50, max(1, (int) ($params['per_page'] ?? 25)));
        $offset = ($page - 1) * $perPage;

        // Treat "global" as null (uses player_global_stats view)
        if ($arena === 'global') {
            $arena = null;
        }

        try {
            $statsRepo = new StatsRepository();

            if ($gameMode !== null) {
                // Per-game-mode aggregation
                $entries = $statsRepo->getLeaderboardByGameMode($gameMode, $sort, $order, $perPage, $offset);
                $total = $statsRepo->getLeaderboardCountByGameMode($gameMode);
                $result = [
                    'entries' => $entries,
                    'total' => $total,
                    'page' => $page,
                    'per_page' => $perPage,
                    'total_pages' => (int) ceil($total / $perPage),
                    'game_mode' => $gameMode,
                ];
            } else {
                // Existing per-arena or global logic
                $entries = $statsRepo->getLeaderboard($sort, $order, $perPage, $offset, $arena);
                $total = $statsRepo->getLeaderboardCount($arena);
                $result = [
                    'entries' => $entries,
                    'total' => $total,
                    'page' => $page,
                    'per_page' => $perPage,
                    'total_pages' => (int) ceil($total / $perPage),
                ];

                // Include arena's game_mode so frontend can adapt columns
                if ($arena !== null) {
                    $arenaRepo = new ArenaRepository();
                    $arenaData = $arenaRepo->findById($arena);
                    $result['game_mode'] = $arenaData['game_mode'] ?? null;
                }
            }

            return $this->success($response, $result);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch leaderboard', 'SERVER_ERROR', 500);
        }
    }

    public function player(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $identifier = $route->getArgument('identifier');

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
            $kitStats = $statsRepo->getKitStats($player['uuid']);
            $recentMatches = $statsRepo->getPlayerRecentMatches($player['uuid']);

            return $this->success($response, [
                'player' => $player,
                'global_stats' => $globalStats,
                'arena_stats' => $arenaStats,
                'kit_stats' => $kitStats,
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
            return $this->success($response, ['matches' => $matches]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch matches', 'SERVER_ERROR', 500);
        }
    }

    public function matchDetails(Request $request, Response $response): Response
    {
        $routeContext = \Slim\Routing\RouteContext::fromRequest($request);
        $route = $routeContext->getRoute();
        $matchId = (int) ($route->getArgument('id', '0'));
        if ($matchId <= 0) {
            return $this->error($response, 'Invalid match ID', 'VALIDATION_ERROR');
        }

        try {
            $matchRepo = new MatchRepository();
            $match = $matchRepo->getMatchWithParticipants($matchId);

            if (!$match) {
                return $this->error($response, 'Match not found', 'NOT_FOUND', 404);
            }

            return $this->success($response, ['match' => $match]);
        } catch (\Throwable $e) {
            return $this->error($response, $e->getMessage(), 'SERVER_ERROR', 500);
        }
    }

    public function arenas(Request $request, Response $response): Response
    {
        try {
            $arenaRepo = new ArenaRepository();
            return $this->success($response, ['arenas' => $arenaRepo->getAll()]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch arenas', 'SERVER_ERROR', 500);
        }
    }

    public function kits(Request $request, Response $response): Response
    {
        try {
            $kitRepo = new KitRepository();
            return $this->success($response, ['kits' => $kitRepo->getVisibleWithStats()]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch kits', 'SERVER_ERROR', 500);
        }
    }

    public function gameModes(Request $request, Response $response): Response
    {
        try {
            $gameModeRepo = new GameModeRepository();
            return $this->success($response, ['game_modes' => $gameModeRepo->getAll()]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch game modes', 'SERVER_ERROR', 500);
        }
    }

    public function submitBugReport(Request $request, Response $response): Response
    {
        $data = $request->getParsedBody();
        if (!$data) {
            $data = json_decode((string) $request->getBody(), true);
        }

        if (empty($data['uuid']) || empty($data['title']) || empty($data['description'])) {
            return $this->error($response, 'Missing required fields: uuid, title, description', 'VALIDATION_ERROR');
        }

        $allowedCategories = ['ui_ux', 'arena', 'kit', 'matchmaking', 'other'];
        $category = $data['category'] ?? 'other';
        if (!in_array($category, $allowedCategories, true)) {
            $category = 'other';
        }

        $uuid = substr((string) $data['uuid'], 0, 36);
        $username = substr((string) ($data['username'] ?? 'Unknown'), 0, 64);
        $title = substr((string) $data['title'], 0, 128);
        $description = substr((string) $data['description'], 0, 5000);

        try {
            $pdo = \App\Database::getConnection();
            $stmt = $pdo->prepare(
                'INSERT INTO `bug_reports` (`player_uuid`, `player_name`, `title`, `category`, `description`) VALUES (?, ?, ?, ?, ?)'
            );
            $stmt->execute([$uuid, $username, $title, $category, $description]);
            $reportId = (int) $pdo->lastInsertId();

            return $this->success($response, ['report_id' => $reportId]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to submit bug report: ' . $e->getMessage(), 'SUBMISSION_ERROR', 500);
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
