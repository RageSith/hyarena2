<?php

namespace App;

use DI\Bridge\Slim\Bridge;
use DI\ContainerBuilder;
use Slim\Views\Twig;
use Slim\Views\TwigMiddleware;

class App
{
    public static function create(): \Slim\App
    {
        $containerBuilder = new ContainerBuilder();

        $containerBuilder->addDefinitions([
            Twig::class => function () {
                $settings = Config::load();
                $cachePath = $settings['twig']['cache'] ?? false;
                $debug = $settings['site']['debug'] ?? false;

                if ($cachePath && !is_dir($cachePath)) {
                    mkdir($cachePath, 0755, true);
                }

                $twig = Twig::create(__DIR__ . '/../templates', [
                    'cache' => $debug ? false : $cachePath,
                    'debug' => $debug,
                    'auto_reload' => true,
                ]);

                $twig->getEnvironment()->addGlobal('site_name', $settings['site']['name'] ?? 'HyArena');
                $twig->getEnvironment()->addGlobal('site_url', $settings['site']['url'] ?? '');

                // Start session only if a session cookie exists (logged-in user)
                if (session_status() === PHP_SESSION_NONE && !empty($_COOKIE[session_name()])) {
                    session_start();
                }
                $twig->getEnvironment()->addGlobal('player_session', !empty($_SESSION['player_account_id'] ?? null));

                return $twig;
            },
        ]);

        $container = $containerBuilder->build();
        $app = Bridge::create($container);

        $app->add(TwigMiddleware::createFromContainer($app, Twig::class));
        $app->addRoutingMiddleware();

        $settings = Config::load();
        $debug = $settings['site']['debug'] ?? false;

        $errorMiddleware = $app->addErrorMiddleware($debug, true, true);

        // Custom error renderer using Twig templates
        $twig = $container->get(Twig::class);
        $errorMiddleware->setDefaultErrorHandler(function (
            \Psr\Http\Message\ServerRequestInterface $request,
            \Throwable $exception,
            bool $displayErrorDetails,
            bool $logErrors,
            bool $logErrorDetails
        ) use ($twig) {
            $statusCode = 500;
            if ($exception instanceof \Slim\Exception\HttpNotFoundException) {
                $statusCode = 404;
            } elseif ($exception instanceof \Slim\Exception\HttpMethodNotAllowedException) {
                $statusCode = 405;
            } elseif ($exception instanceof \Slim\Exception\HttpException) {
                $statusCode = $exception->getCode();
            }

            $response = new \Slim\Psr7\Response();

            // API routes get JSON errors, pages get Twig templates
            if (str_starts_with($request->getUri()->getPath(), '/api/')) {
                $error = ['success' => false, 'error' => [
                    'message' => $displayErrorDetails ? $exception->getMessage() : 'Internal server error',
                    'code' => 'SERVER_ERROR',
                ]];
                $response->getBody()->write(json_encode($error, JSON_UNESCAPED_UNICODE));
                return $response
                    ->withHeader('Content-Type', 'application/json')
                    ->withStatus($statusCode);
            }

            $template = $statusCode === 404 ? 'errors/404.twig' : 'errors/500.twig';

            try {
                return $twig->render($response->withStatus($statusCode), $template);
            } catch (\Throwable $e) {
                $response->getBody()->write("Error {$statusCode}");
                return $response->withStatus($statusCode);
            }
        });

        // Load routes
        $routes = require __DIR__ . '/../config/routes.php';
        $routes($app);

        return $app;
    }
}
