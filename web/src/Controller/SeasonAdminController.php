<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Service\SeasonService;
use App\Repository\ArenaRepository;
use App\Repository\GameModeRepository;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class SeasonAdminController
{
    public function __construct(private Twig $twig) {}

    public function list(Request $request, Response $response): Response
    {
        $service = new SeasonService();
        return $this->twig->render($response, 'admin/seasons.twig', [
            'admin' => AdminAuth::getAdmin(),
            'seasons' => $service->getAllSeasons(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'seasons',
        ]);
    }

    public function createForm(Request $request, Response $response): Response
    {
        $arenaRepo = new ArenaRepository();
        $gameModeRepo = new GameModeRepository();

        return $this->twig->render($response, 'admin/season-form.twig', [
            'admin' => AdminAuth::getAdmin(),
            'season' => null,
            'arenas' => $arenaRepo->getAll(),
            'game_modes' => $gameModeRepo->getAll(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'seasons',
        ]);
    }

    public function create(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();
        $service = new SeasonService();

        $data = $this->parseFormData($body);
        $service->createSeason($data);

        return $response->withHeader('Location', '/admin/seasons')->withStatus(302);
    }

    public function editForm(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $service = new SeasonService();
        $season = $service->getSeasonById($id);
        if (!$season) {
            return $response->withHeader('Location', '/admin/seasons')->withStatus(302);
        }

        $arenaRepo = new ArenaRepository();
        $gameModeRepo = new GameModeRepository();

        return $this->twig->render($response, 'admin/season-form.twig', [
            'admin' => AdminAuth::getAdmin(),
            'season' => $season,
            'arenas' => $arenaRepo->getAll(),
            'game_modes' => $gameModeRepo->getAll(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'seasons',
        ]);
    }

    public function edit(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');
        $body = $request->getParsedBody();

        $service = new SeasonService();
        $data = $this->parseFormData($body);
        $service->updateSeason($id, $data);

        return $response->withHeader('Location', '/admin/seasons')->withStatus(302);
    }

    public function end(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $service = new SeasonService();
        $service->endSeason($id);

        return $response->withHeader('Location', '/admin/seasons')->withStatus(302);
    }

    public function activate(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $service = new SeasonService();
        $service->activateSeason($id);

        return $response->withHeader('Location', '/admin/seasons')->withStatus(302);
    }

    public function archive(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $service = new SeasonService();
        $service->archiveSeason($id);

        return $response->withHeader('Location', '/admin/seasons')->withStatus(302);
    }

    public function delete(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $service = new SeasonService();
        $service->deleteSeason($id);

        return $response->withHeader('Location', '/admin/seasons')->withStatus(302);
    }

    private function parseFormData(array $body): array
    {
        $arenaIds = !empty($body['arena_ids']) ? array_filter($body['arena_ids']) : null;
        $gameModeIds = !empty($body['game_mode_ids']) ? array_filter($body['game_mode_ids']) : null;

        $rankingConfig = null;
        if (($body['ranking_mode'] ?? '') === 'points') {
            $rankingConfig = [
                'points_per_win' => (float) ($body['points_per_win'] ?? 3),
                'points_per_loss' => (float) ($body['points_per_loss'] ?? 1),
                'points_per_kill' => (float) ($body['points_per_kill'] ?? 0),
                'points_per_death' => (float) ($body['points_per_death'] ?? 0),
            ];
        }

        return [
            'name' => $body['name'] ?? '',
            'slug' => $body['slug'] ?? '',
            'description' => $body['description'] ?? null,
            'type' => $body['type'] ?? 'system',
            'starts_at' => $body['starts_at'] ?? '',
            'ends_at' => $body['ends_at'] ?? '',
            'ranking_mode' => $body['ranking_mode'] ?? 'wins',
            'ranking_config' => $rankingConfig,
            'min_matches' => (int) ($body['min_matches'] ?? 5),
            'arena_ids' => $arenaIds,
            'game_mode_ids' => $gameModeIds,
            'visibility' => $body['visibility'] ?? 'public',
            'join_code' => !empty($body['join_code']) ? $body['join_code'] : null,
        ];
    }
}
