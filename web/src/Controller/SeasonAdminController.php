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
        $all = $service->getAllSeasons();

        // Group recurring seasons by base_name chain
        $standalone = [];
        $chains = [];

        foreach ($all as $season) {
            if (empty($season['base_name'])) {
                $standalone[] = $season;
            } else {
                $key = $season['base_name'];
                $chains[$key][] = $season;
            }
        }

        // For each chain: latest (non-archived) first, count archived
        $grouped = [];
        foreach ($chains as $baseName => $members) {
            $active = array_filter($members, fn($s) => $s['status'] !== 'archived');
            $archived = array_filter($members, fn($s) => $s['status'] === 'archived');

            // Sort active by iteration desc
            usort($active, fn($a, $b) => ($b['iteration'] ?? 0) - ($a['iteration'] ?? 0));

            $latest = !empty($active) ? array_shift($active) : null;
            $latest = $latest ?? array_shift($archived);

            if ($latest) {
                $latest['_chain'] = true;
                $latest['_base_name'] = $baseName;
                $latest['_archived_count'] = count($archived) + count($active);
                $latest['_history'] = array_merge($active, $archived);
                $grouped[] = $latest;
            }
        }

        // Merge standalone + grouped, sort by created_at desc
        $seasons = array_merge($standalone, $grouped);
        usort($seasons, fn($a, $b) => strtotime($b['created_at']) - strtotime($a['created_at']));

        return $this->twig->render($response, 'admin/seasons.twig', [
            'admin' => AdminAuth::getAdmin(),
            'seasons' => $seasons,
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
        $this->applyRecurrenceLogic($data);

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
        $this->applyRecurrenceLogic($data);
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

    public function cronLog(Request $request, Response $response): Response
    {
        $logFile = __DIR__ . '/../../logs/season-cron.log';
        $lines = [];

        if (file_exists($logFile)) {
            $all = file($logFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
            $lines = array_slice($all, -200);
        }

        return $this->twig->render($response, 'admin/season-cron-log.twig', [
            'admin' => AdminAuth::getAdmin(),
            'lines' => $lines,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'seasons',
        ]);
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
            'recurrence' => $body['recurrence'] ?? 'none',
            'recurrence_ends_at' => null,
        ];
    }

    private function applyRecurrenceLogic(array &$data): void
    {
        $recurrence = $data['recurrence'] ?? 'none';
        if ($recurrence === 'none') {
            $data['recurrence_ends_at'] = null;
            return;
        }

        // For recurring seasons: form's ends_at = recurrence stop date, period end is auto-calculated
        $data['base_name'] = $data['name'];
        $data['recurrence_ends_at'] = !empty($data['ends_at']) ? $data['ends_at'] : null;
        $data['iteration'] = 1;

        // Auto-calculate actual period ends_at from starts_at + recurrence interval
        $start = new \DateTimeImmutable($data['starts_at']);
        $data['ends_at'] = match ($recurrence) {
            'daily' => $start->modify('+1 day')->format('Y-m-d H:i:s'),
            'weekly' => $start->modify('+7 days')->format('Y-m-d H:i:s'),
            'monthly' => $start->modify('+1 month')->format('Y-m-d H:i:s'),
            'yearly' => $start->modify('+1 year')->format('Y-m-d H:i:s'),
            default => $data['ends_at'],
        };

        $data['name'] = $data['base_name'] . ' #1';
        $data['slug'] = $data['slug'] . '-1';
    }
}
