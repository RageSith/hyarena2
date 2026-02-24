<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Service\ImageService;
use App\Service\TeamService;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class TeamAdminController
{
    public function __construct(private Twig $twig) {}

    public function list(Request $request, Response $response): Response
    {
        $service = new TeamService();
        return $this->twig->render($response, 'admin/team.twig', [
            'admin' => AdminAuth::getAdmin(),
            'members' => $service->getAll(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'team',
        ]);
    }

    public function form(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $member = null;
        $id = $route->getArgument('id');
        if ($id) {
            $service = new TeamService();
            $member = $service->findById((int) $id);
        }

        return $this->twig->render($response, 'admin/team-form.twig', [
            'admin' => AdminAuth::getAdmin(),
            'member' => $member,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'team',
        ]);
    }

    public function create(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();

        $imagePath = null;
        $files = $request->getUploadedFiles();
        if (!empty($files['image']) && $files['image']->getError() === UPLOAD_ERR_OK) {
            $imagePath = ImageService::processTeamImage([
                'tmp_name' => $files['image']->getStream()->getMetadata('uri'),
                'error' => $files['image']->getError(),
            ]);
        }

        $service = new TeamService();
        $service->create([
            'name' => $body['name'] ?? '',
            'role' => $body['role'] ?? '',
            'image_path' => $imagePath,
            'sort_order' => $service->getMaxSortOrder() + 1,
        ]);

        return $response->withHeader('Location', '/admin/team')->withStatus(302);
    }

    public function update(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $body = $request->getParsedBody();
        $id = (int) $route->getArgument('id');

        $service = new TeamService();
        $existing = $service->findById($id);

        $data = [
            'name' => $body['name'] ?? '',
            'role' => $body['role'] ?? '',
        ];

        // Handle image upload / removal
        $files = $request->getUploadedFiles();
        if (!empty($files['image']) && $files['image']->getError() === UPLOAD_ERR_OK) {
            if (!empty($existing['image_path'])) {
                ImageService::deleteTeamImage($existing['image_path']);
            }
            $data['image_path'] = ImageService::processTeamImage([
                'tmp_name' => $files['image']->getStream()->getMetadata('uri'),
                'error' => $files['image']->getError(),
            ]);
        } elseif (!empty($body['remove_image'])) {
            if (!empty($existing['image_path'])) {
                ImageService::deleteTeamImage($existing['image_path']);
            }
            $data['image_path'] = null;
        }

        $service->update($id, $data);

        return $response->withHeader('Location', '/admin/team')->withStatus(302);
    }

    public function delete(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $service = new TeamService();
        $existing = $service->findById($id);
        if (!empty($existing['image_path'])) {
            ImageService::deleteTeamImage($existing['image_path']);
        }

        $service->delete($id);
        return $response->withHeader('Location', '/admin/team')->withStatus(302);
    }

    public function moveUp(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $service = new TeamService();
        $service->moveUp($id);

        return $response->withHeader('Location', '/admin/team')->withStatus(302);
    }

    public function moveDown(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $service = new TeamService();
        $service->moveDown($id);

        return $response->withHeader('Location', '/admin/team')->withStatus(302);
    }
}
