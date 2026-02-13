<?php

namespace App\Middleware;

use App\Config;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Psr\Http\Server\MiddlewareInterface;
use Psr\Http\Server\RequestHandlerInterface as Handler;
use Slim\Psr7\Response as SlimResponse;

class RateLimitMiddleware implements MiddlewareInterface
{
    public function process(Request $request, Handler $handler): Response
    {
        $ip = $request->getServerParams()['REMOTE_ADDR'] ?? '127.0.0.1';
        $limit = Config::get('rate_limit.requests_per_minute', 60);
        $storagePath = Config::get('rate_limit.storage_path', sys_get_temp_dir() . '/hyarena_rate_limit');

        if (!is_dir($storagePath)) {
            mkdir($storagePath, 0755, true);
        }

        $file = $storagePath . '/' . md5($ip) . '.json';
        $now = time();
        $windowStart = $now - 60;

        $requests = [];
        if (file_exists($file)) {
            $data = json_decode(file_get_contents($file), true);
            if (is_array($data)) {
                $requests = array_filter($data, fn($ts) => $ts > $windowStart);
            }
        }

        if (count($requests) >= $limit) {
            $response = new SlimResponse();
            $response->getBody()->write(json_encode([
                'success' => false,
                'error' => ['message' => 'Rate limit exceeded', 'code' => 'RATE_LIMITED'],
            ]));
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withHeader('Retry-After', '60')
                ->withStatus(429);
        }

        $requests[] = $now;
        file_put_contents($file, json_encode(array_values($requests)));

        return $handler->handle($request);
    }
}
