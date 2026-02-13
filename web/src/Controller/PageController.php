<?php

namespace App\Controller;

use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class PageController
{
    public function __construct(private Twig $twig) {}

    public function home(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/home.twig', [
            'active_page' => 'home',
        ]);
    }

    public function leaderboard(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/leaderboard.twig', [
            'active_page' => 'leaderboard',
        ]);
    }

    public function player(Request $request, Response $response, array $args): Response
    {
        return $this->twig->render($response, 'pages/player.twig', [
            'player_name' => $args['name'],
        ]);
    }

    public function legal(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/legal.twig');
    }

    public function privacy(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/privacy.twig');
    }
}
