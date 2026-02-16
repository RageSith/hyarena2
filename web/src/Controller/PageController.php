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

    public function live(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/live.twig', [
            'active_page' => 'live',
        ]);
    }

    public function arenas(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/arenas.twig', [
            'active_page' => 'arenas',
        ]);
    }

    public function kits(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/kits.twig', [
            'active_page' => 'kits',
        ]);
    }

    public function features(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/features.twig', [
            'active_page' => 'features',
        ]);
    }

    public function leaderboard(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/leaderboard.twig', [
            'active_page' => 'leaderboard',
        ]);
    }

    public function seasons(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/seasons.twig', [
            'active_page' => 'seasons',
        ]);
    }

    public function seasonsArchive(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'pages/seasons-archive.twig', [
            'active_page' => 'seasons',
        ]);
    }

    public function seasonDetail(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        return $this->twig->render($response, 'pages/season-detail.twig', [
            'active_page' => 'seasons',
            'season_slug' => $route->getArgument('slug'),
        ]);
    }

    public function player(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        return $this->twig->render($response, 'pages/player.twig', [
            'player_name' => $route->getArgument('name'),
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
