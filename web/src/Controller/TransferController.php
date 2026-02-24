<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Service\HywardenClient;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class TransferController
{
    public function __construct(private Twig $twig) {}

    // ==========================================
    // Page
    // ==========================================

    public function page(Request $request, Response $response): Response
    {
        $client = new HywardenClient();
        $servers = $client->getServers();

        return $this->twig->render($response, 'admin/transfer.twig', [
            'admin' => AdminAuth::getAdmin(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'transfer',
            'servers' => isset($servers['error']) ? [] : $servers,
            'hw_error' => $servers['error'] ?? null,
        ]);
    }

    // ==========================================
    // Proxy API
    // ==========================================

    public function apiGameData(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');

        $client = new HywardenClient();
        $result = $client->getGameData($id);

        $status = isset($result['error']) ? 400 : 200;
        return $this->json($response, $result)->withStatus($status);
    }

    public function apiBackups(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');

        $client = new HywardenClient();
        $result = $client->getGameDataBackups($id);

        $status = isset($result['error']) ? 400 : 200;
        return $this->json($response, $result)->withStatus($status);
    }

    public function apiTransfer(Request $request, Response $response): Response
    {
        $body = json_decode((string) $request->getBody(), true);
        if (!$body) {
            return $this->json($response, ['error' => 'Invalid request body'])->withStatus(400);
        }

        $client = new HywardenClient();
        $result = $client->transfer($body);

        $status = isset($result['error']) ? 400 : 200;
        return $this->json($response, $result)->withStatus($status);
    }

    public function apiBackupAction(Request $request, Response $response): Response
    {
        $body = json_decode((string) $request->getBody(), true);
        if (!$body) {
            return $this->json($response, ['error' => 'Invalid request body'])->withStatus(400);
        }

        $client = new HywardenClient();
        $result = $client->transferBackup($body);

        $status = isset($result['error']) ? 400 : 200;
        return $this->json($response, $result)->withStatus($status);
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
