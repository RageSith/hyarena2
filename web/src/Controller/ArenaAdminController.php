<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Repository\ArenaRepository;
use App\Service\ImageService;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class ArenaAdminController
{
    public function __construct(private Twig $twig) {}

    public function list(Request $request, Response $response): Response
    {
        $repo = new ArenaRepository();
        $arenas = $repo->getAllAdmin();

        $success = $request->getQueryParams()['success'] ?? null;

        return $this->twig->render($response, 'admin/arenas.twig', [
            'arenas' => $arenas,
            'success' => $success,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'arenas',
        ]);
    }

    public function edit(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = $route->getArgument('id');
        $repo = new ArenaRepository();
        $arena = $repo->findById($id);

        if (!$arena) {
            return $response->withHeader('Location', '/admin/arenas?success=' . urlencode('Arena not found.'))->withStatus(302);
        }

        $body = $request->getParsedBody();

        // Handle shown toggle
        $shown = !empty($body['shown']);
        $repo->updateShown($id, $shown);

        // Handle remove image
        if (!empty($body['remove_image']) && !empty($arena['icon'])) {
            ImageService::deleteArenaImage($arena['icon']);
            $repo->updateIcon($id, null);
            return $response->withHeader('Location', '/admin/arenas?success=' . urlencode('Image removed for ' . $arena['display_name'] . '.'))->withStatus(302);
        }

        // Handle image upload
        $files = $request->getUploadedFiles();
        if (!empty($files['image']) && $files['image']->getError() === UPLOAD_ERR_OK) {
            // Delete old image if exists
            if (!empty($arena['icon'])) {
                ImageService::deleteArenaImage($arena['icon']);
            }

            $filename = ImageService::processArenaImage([
                'tmp_name' => $files['image']->getStream()->getMetadata('uri'),
                'error' => $files['image']->getError(),
            ], $arena['display_name']);

            if ($filename) {
                $repo->updateIcon($id, $filename);
                return $response->withHeader('Location', '/admin/arenas?success=' . urlencode('Image updated for ' . $arena['display_name'] . '.'))->withStatus(302);
            }
        }

        return $response->withHeader('Location', '/admin/arenas?success=' . urlencode('Settings saved for ' . $arena['display_name'] . '.'))->withStatus(302);
    }
}
