<?php

namespace App\Middleware;

use App\Config;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface as Handler;
use Slim\Psr7\Response as SlimResponse;

class ApiKeyMiddleware implements MiddlewareInterface
{
    public function process(Request $request, Handler $handler): Response
    {
        $apiKey = $request->getHeaderLine('X-API-Key')
            ?: ($request->getQueryParams()['api_key'] ?? '');

        $expectedKey = Config::get('api.key', '');

        if ($apiKey === '' || !hash_equals($expectedKey, $apiKey)) {
            $response = new SlimResponse();
            $response->getBody()->write(json_encode([
                'success' => false,
                'error' => ['message' => 'Invalid API key', 'code' => 'UNAUTHORIZED'],
            ]));
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(401);
        }

        return $handler->handle($request);
    }
}
