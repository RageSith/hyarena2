<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Auth\AdminPermissions;
use App\Repository\PlayerRepository;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class PlayerManagementController
{
    public function __construct(private Twig $twig) {}

    public function list(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('players');

        $params = $request->getQueryParams();
        $query = $params['q'] ?? null;
        $page = max(1, (int) ($params['page'] ?? 1));
        $limit = 20;
        $offset = ($page - 1) * $limit;

        $repo = new PlayerRepository();
        $players = $repo->searchPlayers($query, $limit, $offset);
        $total = $repo->getPlayerCount($query);

        return $this->twig->render($response, 'admin/players.twig', [
            'admin' => AdminAuth::getAdmin(),
            'players' => $players,
            'query' => $query,
            'page' => $page,
            'total_pages' => max(1, ceil($total / $limit)),
            'total' => $total,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'players',
        ]);
    }

    public function detail(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('players');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $uuid = $route->getArgument('uuid');

        $repo = new PlayerRepository();
        $player = $repo->getPlayerDetail($uuid);
        if (!$player) {
            return $response->withHeader('Location', '/admin/players')->withStatus(302);
        }

        return $this->twig->render($response, 'admin/player-detail.twig', [
            'admin' => AdminAuth::getAdmin(),
            'player' => $player,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'players',
        ]);
    }

    public function ban(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('players');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $uuid = $route->getArgument('uuid');
        $body = $request->getParsedBody();
        $admin = AdminAuth::getAdmin();

        $reason = trim($body['reason'] ?? '');
        if (empty($reason)) {
            $reason = 'No reason provided';
        }

        $repo = new PlayerRepository();
        $repo->banPlayer($uuid, $reason, $admin['id']);

        return $response->withHeader('Location', '/admin/players/' . $uuid)->withStatus(302);
    }

    public function unban(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('players');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $uuid = $route->getArgument('uuid');

        $repo = new PlayerRepository();
        $repo->unbanPlayer($uuid);

        return $response->withHeader('Location', '/admin/players/' . $uuid)->withStatus(302);
    }
}
