<?php

namespace App\Middleware;

use App\Auth\AdminAuth;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface as Handler;
use Slim\Psr7\Response as SlimResponse;

class CsrfMiddleware implements MiddlewareInterface
{
    public function process(Request $request, Handler $handler): Response
    {
        if ($request->getMethod() === 'POST') {
            $body = $request->getParsedBody();
            $token = $body['_csrf_token'] ?? '';
            if ($token === '') {
                $token = $request->getHeaderLine('X-CSRF-Token');
            }

            if (!AdminAuth::validateCsrfToken($token)) {
                $response = new SlimResponse();
                return $response
                    ->withHeader('Location', '/admin/login')
                    ->withStatus(302);
            }
        }

        return $handler->handle($request);
    }
}
