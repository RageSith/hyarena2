<?php

use App\Controller\PageController;
use App\Controller\ApiController;
use App\Controller\AdminController;
use App\Controller\AdminUserController;
use App\Controller\BugReportAdminController;
use App\Controller\LinkController;
use App\Controller\PlayerManagementController;
use App\Controller\SeasonApiController;
use App\Controller\SeasonAdminController;
use App\Middleware\ApiKeyMiddleware;
use App\Middleware\RateLimitMiddleware;
use App\Middleware\CorsMiddleware;
use App\Middleware\AdminAuthMiddleware;
use App\Middleware\AdminRoleMiddleware;
use App\Middleware\PlayerAuthMiddleware;
use App\Middleware\CsrfMiddleware;
use Slim\App;
use Slim\Routing\RouteCollectorProxy;

return function (App $app) {
    // Public pages
    $app->get('/', [PageController::class, 'home']);
    $app->get('/news', [PageController::class, 'news']);
    $app->get('/live', [PageController::class, 'live']);
    $app->get('/arenas', [PageController::class, 'arenas']);
    $app->get('/kits', [PageController::class, 'kits']);
    $app->get('/features', [PageController::class, 'features']);
    $app->get('/leaderboard', [PageController::class, 'leaderboard']);
    $app->get('/seasons', [PageController::class, 'seasons']);
    $app->get('/seasons/archive', [PageController::class, 'seasonsArchive']);
    $app->get('/seasons/{slug}', [PageController::class, 'seasonDetail']);
    $app->get('/player/{name}', [PageController::class, 'player']);
    $app->get('/legal', [PageController::class, 'legal']);
    $app->get('/privacy', [PageController::class, 'privacy']);

    // Public API (rate limited)
    $app->group('/api', function (RouteCollectorProxy $group) {
        $group->get('/server-stats', [ApiController::class, 'serverStats']);
        $group->get('/leaderboard', [ApiController::class, 'leaderboard']);
        $group->get('/player/{identifier}', [ApiController::class, 'player']);
        $group->get('/recent-matches', [ApiController::class, 'recentMatches']);
        $group->get('/match/{id}', [ApiController::class, 'matchDetails']);
        $group->get('/arenas', [ApiController::class, 'arenas']);
        $group->get('/kits', [ApiController::class, 'kits']);
        $group->get('/game-modes', [ApiController::class, 'gameModes']);
        $group->get('/seasons', [SeasonApiController::class, 'listActive']);
        $group->get('/seasons/archive', [SeasonApiController::class, 'listEnded']);
        $group->get('/seasons/{slug}', [SeasonApiController::class, 'detail']);
        $group->get('/seasons/{slug}/leaderboard', [SeasonApiController::class, 'leaderboard']);
        $group->get('/player/{identifier}/seasons', [SeasonApiController::class, 'playerHistory']);
    })->add(new RateLimitMiddleware())->add(new CorsMiddleware());

    // Plugin API (API key protected)
    $app->group('/api', function (RouteCollectorProxy $group) {
        $group->post('/match/submit', [ApiController::class, 'submitMatch']);
        $group->post('/sync', [ApiController::class, 'sync']);
        $group->post('/player/sync', [ApiController::class, 'playerSync']);
        $group->post('/link/generate', [LinkController::class, 'generateCode']);
        $group->post('/bug/submit', [ApiController::class, 'submitBugReport']);
    })->add(new ApiKeyMiddleware())->add(new CorsMiddleware());

    // Player auth pages
    $app->get('/register', [LinkController::class, 'registerPage']);
    $app->post('/register', [LinkController::class, 'register']);
    $app->get('/login', [LinkController::class, 'loginPage']);
    $app->post('/login', [LinkController::class, 'login']);
    $app->get('/logout', [LinkController::class, 'logout']);

    // Player authenticated pages
    $app->group('', function (RouteCollectorProxy $group) {
        $group->get('/link', [LinkController::class, 'linkPage']);
        $group->post('/link', [LinkController::class, 'link']);
        $group->get('/profile', [LinkController::class, 'profile']);
        $group->post('/profile/season/join', [LinkController::class, 'joinSeason']);
        $group->post('/profile/season/opt-out', [LinkController::class, 'optOutSeason']);
        $group->get('/profile/password', [LinkController::class, 'changePasswordPage']);
        $group->post('/profile/password', [LinkController::class, 'changePassword']);
    })->add(new PlayerAuthMiddleware());

    // Admin routes
    $app->get('/admin/login', [AdminController::class, 'loginPage']);
    $app->post('/admin/login', [AdminController::class, 'login']);

    $app->group('/admin', function (RouteCollectorProxy $group) {
        $group->get('', [AdminController::class, 'dashboard']);
        $group->get('/logout', [AdminController::class, 'logout']);

        // Bug Reports
        $group->get('/bug-reports', [BugReportAdminController::class, 'list']);
        $group->get('/bug-reports/{id}', [BugReportAdminController::class, 'detail']);
        $group->post('/bug-reports/{id}/status', [BugReportAdminController::class, 'updateStatus']);
        $group->post('/bug-reports/{id}/delete', [BugReportAdminController::class, 'delete']);

        // Players
        $group->get('/players', [PlayerManagementController::class, 'list']);
        $group->get('/players/{uuid}', [PlayerManagementController::class, 'detail']);
        $group->post('/players/{uuid}/ban', [PlayerManagementController::class, 'ban']);
        $group->post('/players/{uuid}/unban', [PlayerManagementController::class, 'unban']);

        // Notifications (admin+ only)
        $group->group('', function (RouteCollectorProxy $inner) {
            $inner->get('/notifications', [AdminController::class, 'notifications']);
            $inner->get('/notifications/create', [AdminController::class, 'notificationForm']);
            $inner->post('/notifications/create', [AdminController::class, 'createNotification']);
            $inner->get('/notifications/{id}/edit', [AdminController::class, 'notificationForm']);
            $inner->post('/notifications/{id}/edit', [AdminController::class, 'updateNotification']);
            $inner->post('/notifications/{id}/delete', [AdminController::class, 'deleteNotification']);
        })->add(new AdminRoleMiddleware('notifications'));

        // Webhooks (super_admin only)
        $group->group('', function (RouteCollectorProxy $inner) {
            $inner->get('/webhooks', [AdminController::class, 'webhooks']);
            $inner->get('/webhooks/create', [AdminController::class, 'webhookForm']);
            $inner->post('/webhooks/create', [AdminController::class, 'createWebhook']);
            $inner->get('/webhooks/{id}/edit', [AdminController::class, 'webhookForm']);
            $inner->post('/webhooks/{id}/edit', [AdminController::class, 'updateWebhook']);
            $inner->post('/webhooks/{id}/delete', [AdminController::class, 'deleteWebhook']);
        })->add(new AdminRoleMiddleware('webhooks'));

        // Seasons (admin+ only)
        $group->group('', function (RouteCollectorProxy $inner) {
            $inner->get('/seasons', [SeasonAdminController::class, 'list']);
            $inner->get('/seasons/cron-log', [SeasonAdminController::class, 'cronLog']);
            $inner->get('/seasons/create', [SeasonAdminController::class, 'createForm']);
            $inner->post('/seasons/create', [SeasonAdminController::class, 'create']);
            $inner->get('/seasons/{id}/edit', [SeasonAdminController::class, 'editForm']);
            $inner->post('/seasons/{id}/edit', [SeasonAdminController::class, 'edit']);
            $inner->post('/seasons/{id}/activate', [SeasonAdminController::class, 'activate']);
            $inner->post('/seasons/{id}/end', [SeasonAdminController::class, 'end']);
            $inner->post('/seasons/{id}/archive', [SeasonAdminController::class, 'archive']);
            $inner->post('/seasons/{id}/delete', [SeasonAdminController::class, 'delete']);
        })->add(new AdminRoleMiddleware('seasons'));

        // Admin Users (super_admin only)
        $group->group('', function (RouteCollectorProxy $inner) {
            $inner->get('/users', [AdminUserController::class, 'list']);
            $inner->get('/users/create', [AdminUserController::class, 'createForm']);
            $inner->post('/users/create', [AdminUserController::class, 'create']);
            $inner->get('/users/{id}/edit', [AdminUserController::class, 'editForm']);
            $inner->post('/users/{id}/edit', [AdminUserController::class, 'edit']);
            $inner->post('/users/{id}/delete', [AdminUserController::class, 'delete']);
        })->add(new AdminRoleMiddleware('admin_users'));
    })->add(new CsrfMiddleware())->add(new AdminAuthMiddleware());
};
