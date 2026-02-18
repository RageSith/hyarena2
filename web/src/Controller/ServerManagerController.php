<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Service\HywardenClient;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class ServerManagerController
{
    public function __construct(private Twig $twig) {}

    // ==========================================
    // Page
    // ==========================================

    public function page(Request $request, Response $response): Response
    {
        $client = new HywardenClient();
        $metrics = $client->getMetrics();
        $servers = $client->getServers();

        return $this->twig->render($response, 'admin/servers.twig', [
            'admin' => AdminAuth::getAdmin(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'servers',
            'metrics' => $metrics,
            'servers' => isset($servers['error']) ? [] : $servers,
            'hw_error' => $metrics['error'] ?? $servers['error'] ?? null,
        ]);
    }

    // ==========================================
    // Proxy API — JSON
    // ==========================================

    public function apiServers(Request $request, Response $response): Response
    {
        $client = new HywardenClient();
        return $this->json($response, $client->getServers());
    }

    public function apiMetrics(Request $request, Response $response): Response
    {
        $client = new HywardenClient();
        return $this->json($response, $client->getMetrics());
    }

    public function apiConsole(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');
        $params = $request->getQueryParams();
        $offset = isset($params['offset']) ? (int) $params['offset'] : -1;
        $limit = isset($params['limit']) ? (int) $params['limit'] : 500;

        $client = new HywardenClient();
        return $this->json($response, $client->getConsole($id, $offset, $limit));
    }

    public function apiStart(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $client = new HywardenClient();
        return $this->json($response, $client->startServer($route->getArgument('id')));
    }

    public function apiStop(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $client = new HywardenClient();
        return $this->json($response, $client->stopServer($route->getArgument('id')));
    }

    public function apiKill(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $client = new HywardenClient();
        return $this->json($response, $client->killServer($route->getArgument('id')));
    }

    // ==========================================
    // Helpers
    // ==========================================

    private function json(Response $response, array $data): Response
    {
        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
