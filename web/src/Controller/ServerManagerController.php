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

    public function apiReauth(Request $request, Response $response): Response
    {
        $client = new HywardenClient();
        return $this->json($response, $client->reauth());
    }

    // ==========================================
    // Proxy API — Prefabs
    // ==========================================

    public function apiListPrefabs(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $client = new HywardenClient();
        return $this->json($response, $client->listPrefabs($route->getArgument('id')));
    }

    public function apiUploadPrefab(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');

        $files = $request->getUploadedFiles();
        $file = $files['file'] ?? null;

        if (!$file || $file->getError() !== UPLOAD_ERR_OK) {
            return $this->json($response, ['error' => 'No file uploaded'])->withStatus(400);
        }

        $filename = $file->getClientFilename();
        if (!str_ends_with(strtolower($filename), '.prefab.json')) {
            return $this->json($response, ['error' => 'File must end with .prefab.json'])->withStatus(400);
        }

        $tmp = tempnam(sys_get_temp_dir(), 'prefab_');
        $file->moveTo($tmp);

        $client = new HywardenClient();
        $result = $client->uploadPrefab($id, $tmp, $filename);
        @unlink($tmp);

        $status = isset($result['error']) ? 400 : 200;
        return $this->json($response, $result)->withStatus($status);
    }

    public function apiDeletePrefab(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');
        $name = $route->getArgument('name');

        $client = new HywardenClient();
        $result = $client->deletePrefab($id, $name);

        $status = isset($result['error']) ? 400 : 200;
        return $this->json($response, $result)->withStatus($status);
    }

    public function apiDownloadPrefab(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');
        $name = $route->getArgument('name');

        $client = new HywardenClient();
        $result = $client->downloadPrefab($id, $name);

        if (isset($result['error'])) {
            return $this->json($response, $result)->withStatus(400);
        }

        $response->getBody()->write($result['body']);
        return $response
            ->withHeader('Content-Type', $result['headers']['content-type'] ?? 'application/octet-stream')
            ->withHeader('Content-Disposition', 'attachment; filename="' . $name . '"')
            ->withHeader('Content-Length', (string) strlen($result['body']));
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
