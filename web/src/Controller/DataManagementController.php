<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Auth\AdminPermissions;
use App\Repository\DataManagementRepository;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class DataManagementController
{
    public function __construct(private Twig $twig) {}

    public function index(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $repo = new DataManagementRepository();
        $params = $request->getQueryParams();

        return $this->twig->render($response, 'admin/data-management.twig', [
            'admin' => AdminAuth::getAdmin(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'data_management',
            'counts' => $repo->getSummaryCounts(),
            'orphaned_arenas' => $repo->getOrphanedArenas(),
            'orphaned_kits' => $repo->getOrphanedKits(),
            'orphaned_game_modes' => $repo->getOrphanedGameModes(),
            'success' => $params['success'] ?? null,
        ]);
    }

    public function resetStats(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $body = $request->getParsedBody();
        $confirmation = trim($body['confirmation'] ?? '');

        if ($confirmation === 'RESET') {
            $repo = new DataManagementRepository();
            $repo->resetAllStats();
            return $response->withHeader('Location', '/admin/data-management?success=stats_reset')->withStatus(302);
        }

        return $response->withHeader('Location', '/admin/data-management')->withStatus(302);
    }

    public function fullPurge(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $body = $request->getParsedBody();
        $confirmation = trim($body['confirmation'] ?? '');

        if ($confirmation === 'PURGE ALL DATA') {
            $repo = new DataManagementRepository();
            $repo->fullDataPurge();
            return $response->withHeader('Location', '/admin/data-management?success=full_purge')->withStatus(302);
        }

        return $response->withHeader('Location', '/admin/data-management')->withStatus(302);
    }

    public function deleteArena(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');

        $repo = new DataManagementRepository();
        $repo->deleteOrphanedArena($id);

        return $response->withHeader('Location', '/admin/data-management?success=arena_deleted')->withStatus(302);
    }

    public function deleteAllArenas(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $repo = new DataManagementRepository();
        $count = $repo->deleteAllOrphanedArenas();

        return $response->withHeader('Location', '/admin/data-management?success=arenas_deleted&count=' . $count)->withStatus(302);
    }

    public function deleteKit(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');

        $repo = new DataManagementRepository();
        $repo->deleteOrphanedKit($id);

        return $response->withHeader('Location', '/admin/data-management?success=kit_deleted')->withStatus(302);
    }

    public function deleteAllKits(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $repo = new DataManagementRepository();
        $count = $repo->deleteAllOrphanedKits();

        return $response->withHeader('Location', '/admin/data-management?success=kits_deleted&count=' . $count)->withStatus(302);
    }

    public function deleteGameMode(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');

        $repo = new DataManagementRepository();
        $repo->deleteOrphanedGameMode($id);

        return $response->withHeader('Location', '/admin/data-management?success=gamemode_deleted')->withStatus(302);
    }

    public function deleteAllGameModes(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('data_management');

        $repo = new DataManagementRepository();
        $count = $repo->deleteAllOrphanedGameModes();

        return $response->withHeader('Location', '/admin/data-management?success=gamemodes_deleted&count=' . $count)->withStatus(302);
    }
}
