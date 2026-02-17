<?php

namespace App\Controller;

use App\Service\SeasonService;
use App\Repository\PlayerRepository;
use App\Repository\SeasonRepository;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class SeasonApiController
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

    public function listActive(Request $request, Response $response): Response
    {
        try {
            $service = new SeasonService();
            $playerUuid = $_SESSION['player_uuid'] ?? null;
            $seasons = $service->getActiveSeasonsForPlayer($playerUuid);
            return $this->success($response, ['seasons' => $seasons]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch seasons', 'SERVER_ERROR', 500);
        }
    }

    public function listEnded(Request $request, Response $response): Response
    {
        $params = $request->getQueryParams();
        $page = max(1, (int) ($params['page'] ?? 1));
        $perPage = min(50, max(1, (int) ($params['per_page'] ?? 25)));
        $offset = ($page - 1) * $perPage;

        try {
            $service = new SeasonService();
            $seasons = $service->getEndedSeasons($perPage, $offset);
            $total = $service->getEndedSeasonsCount();

            return $this->success($response, [
                'seasons' => $seasons,
                'total' => $total,
                'page' => $page,
                'per_page' => $perPage,
                'total_pages' => (int) ceil($total / $perPage),
            ]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch archived seasons', 'SERVER_ERROR', 500);
        }
    }

    public function detail(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $slug = $route->getArgument('slug');

        try {
            $service = new SeasonService();
            $season = $service->getSeasonBySlug($slug);

            if (!$season) {
                return $this->error($response, 'Season not found', 'NOT_FOUND', 404);
            }

            // Private season restriction: strip ranking details for non-participants
            if ($season['visibility'] !== 'public') {
                $playerUuid = $_SESSION['player_uuid'] ?? null;
                $repo = new SeasonRepository();
                $isEnrolled = $playerUuid && $repo->isParticipant((int) $season['id'], $playerUuid);

                if (!$isEnrolled) {
                    $season = [
                        'name' => $season['name'],
                        'slug' => $season['slug'],
                        'description' => $season['description'],
                        'status' => $season['status'],
                        'starts_at' => $season['starts_at'],
                        'ends_at' => $season['ends_at'],
                        'participant_count' => $season['participant_count'],
                        'visibility' => $season['visibility'],
                        'restricted' => true,
                    ];
                    return $this->success($response, ['season' => $season]);
                }
            }

            $season['restricted'] = false;
            return $this->success($response, ['season' => $season]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch season', 'SERVER_ERROR', 500);
        }
    }

    public function leaderboard(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $slug = $route->getArgument('slug');
        $params = $request->getQueryParams();
        $page = max(1, (int) ($params['page'] ?? 1));
        $perPage = min(50, max(1, (int) ($params['per_page'] ?? 25)));
        $offset = ($page - 1) * $perPage;
        $order = $params['order'] ?? 'DESC';

        try {
            $service = new SeasonService();
            $season = $service->getSeasonBySlug($slug);

            if (!$season) {
                return $this->error($response, 'Season not found', 'NOT_FOUND', 404);
            }

            // Block leaderboard access for non-participants of private seasons
            if ($season['visibility'] !== 'public') {
                $playerUuid = $_SESSION['player_uuid'] ?? null;
                $repo = new SeasonRepository();
                $isEnrolled = $playerUuid && $repo->isParticipant((int) $season['id'], $playerUuid);

                if (!$isEnrolled) {
                    return $this->error($response, 'This season is private. Only participants can view rankings.', 'SEASON_PRIVATE', 403);
                }
            }

            $seasonId = (int) $season['id'];
            $minMatches = (int) $season['min_matches'];
            $isEnded = in_array($season['status'], ['ended', 'archived']);

            if ($isEnded) {
                $entries = $service->getFrozenRankings($seasonId, $perPage, $offset);
                $total = $service->getFrozenRankingsCount($seasonId);
            } else {
                $entries = $service->getSeasonLeaderboard($seasonId, $season['ranking_mode'], $order, $perPage, $offset, $minMatches);
                $total = $service->getSeasonLeaderboardCount($seasonId, $minMatches);
            }

            return $this->success($response, [
                'entries' => $entries,
                'total' => $total,
                'page' => $page,
                'per_page' => $perPage,
                'total_pages' => (int) ceil($total / $perPage),
                'is_frozen' => $isEnded,
                'ranking_mode' => $season['ranking_mode'],
                'min_matches' => $minMatches,
            ]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch season leaderboard', 'SERVER_ERROR', 500);
        }
    }

    public function playerHistory(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $identifier = $route->getArgument('identifier');

        try {
            $playerRepo = new PlayerRepository();
            $player = strlen($identifier) === 36
                ? $playerRepo->findByUuid($identifier)
                : $playerRepo->findByUsername($identifier);

            if (!$player) {
                return $this->error($response, 'Player not found', 'NOT_FOUND', 404);
            }

            $service = new SeasonService();
            $history = $service->getPlayerSeasonHistory($player['uuid']);

            return $this->success($response, ['seasons' => $history]);
        } catch (\Exception $e) {
            return $this->error($response, 'Failed to fetch player season history', 'SERVER_ERROR', 500);
        }
    }
}
