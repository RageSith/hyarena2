<?php

namespace App\Middleware;

use App\Auth\AdminAuth;
use App\Auth\AdminPermissions;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface as Handler;
use Slim\Psr7\Response as SlimResponse;

class AdminRoleMiddleware implements MiddlewareInterface
{
    private string $section;

    public function __construct(string $section)
    {
        $this->section = $section;
    }

    public function process(Request $request, Handler $handler): Response
    {
        $admin = AdminAuth::getAdmin();
        if (!$admin || !AdminPermissions::can($admin['role'], $this->section)) {
            $response = new SlimResponse();
            $response->getBody()->write('<h1>403 Forbidden</h1><p>You do not have permission to access this section.</p>');
            return $response->withStatus(403);
        }

        return $handler->handle($request);
    }
}
