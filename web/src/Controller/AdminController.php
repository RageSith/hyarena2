<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Repository\AdminRepository;
use App\Repository\BugReportRepository;
use App\Service\NotificationService;
use App\Service\WebhookService;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class AdminController
{
    public function __construct(private Twig $twig) {}

    // ==========================================
    // Auth
    // ==========================================

    public function loginPage(Request $request, Response $response): Response
    {
        if (AdminAuth::isLoggedIn()) {
            return $response->withHeader('Location', '/admin')->withStatus(302);
        }
        return $this->twig->render($response, 'admin/login.twig');
    }

    public function login(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();
        $ip = $request->getServerParams()['REMOTE_ADDR'] ?? '127.0.0.1';

        $result = AdminAuth::login(
            $body['username'] ?? '',
            $body['password'] ?? '',
            $ip
        );

        if ($result['success']) {
            return $response->withHeader('Location', '/admin')->withStatus(302);
        }

        return $this->twig->render($response, 'admin/login.twig', [
            'error' => $result['error'],
        ]);
    }

    public function logout(Request $request, Response $response): Response
    {
        AdminAuth::logout();
        return $response->withHeader('Location', '/admin/login')->withStatus(302);
    }

    // ==========================================
    // Dashboard
    // ==========================================

    public function dashboard(Request $request, Response $response): Response
    {
        $repo = new AdminRepository();
        $stats = $repo->getDashboardStats();

        $bugRepo = new BugReportRepository();
        $bugCounts = $bugRepo->getCountByStatus();
        $stats['open_bugs'] = $bugCounts['open'];

        return $this->twig->render($response, 'admin/dashboard.twig', [
            'admin' => AdminAuth::getAdmin(),
            'stats' => $stats,
            'open_bug_count' => $bugCounts['open'],
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'dashboard',
        ]);
    }

    // ==========================================
    // Notifications
    // ==========================================

    public function notifications(Request $request, Response $response): Response
    {
        $service = new NotificationService();
        return $this->twig->render($response, 'admin/notifications.twig', [
            'admin' => AdminAuth::getAdmin(),
            'notifications' => $service->getAll(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'notifications',
        ]);
    }

    public function notificationForm(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $notification = null;
        $id = $route->getArgument('id');
        if ($id) {
            $service = new NotificationService();
            $notification = $service->findById((int) $id);
        }

        return $this->twig->render($response, 'admin/notification-form.twig', [
            'admin' => AdminAuth::getAdmin(),
            'notification' => $notification,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'notifications',
        ]);
    }

    public function createNotification(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();
        $admin = AdminAuth::getAdmin();

        $service = new NotificationService();
        $service->create([
            'title' => $body['title'] ?? '',
            'message' => $body['message'] ?? '',
            'type' => $body['type'] ?? 'info',
            'is_active' => isset($body['is_active']) ? 1 : 0,
            'starts_at' => $body['starts_at'] ?? null,
            'ends_at' => $body['ends_at'] ?? null,
            'created_by' => $admin['id'],
        ]);

        return $response->withHeader('Location', '/admin/notifications')->withStatus(302);
    }

    public function updateNotification(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $body = $request->getParsedBody();

        $service = new NotificationService();
        $service->update((int) $route->getArgument('id'), [
            'title' => $body['title'] ?? '',
            'message' => $body['message'] ?? '',
            'type' => $body['type'] ?? 'info',
            'is_active' => isset($body['is_active']) ? 1 : 0,
            'starts_at' => $body['starts_at'] ?? null,
            'ends_at' => $body['ends_at'] ?? null,
        ]);

        return $response->withHeader('Location', '/admin/notifications')->withStatus(302);
    }

    public function deleteNotification(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $service = new NotificationService();
        $service->delete((int) $route->getArgument('id'));
        return $response->withHeader('Location', '/admin/notifications')->withStatus(302);
    }

    // ==========================================
    // Webhooks
    // ==========================================

    public function webhooks(Request $request, Response $response): Response
    {
        $service = new WebhookService();
        return $this->twig->render($response, 'admin/webhooks.twig', [
            'admin' => AdminAuth::getAdmin(),
            'webhooks' => $service->getAll(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'webhooks',
        ]);
    }

    public function webhookForm(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $webhook = null;
        $id = $route->getArgument('id');
        if ($id) {
            $service = new WebhookService();
            $webhook = $service->findById((int) $id);
        }

        return $this->twig->render($response, 'admin/webhook-form.twig', [
            'admin' => AdminAuth::getAdmin(),
            'webhook' => $webhook,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'webhooks',
        ]);
    }

    public function createWebhook(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();

        $service = new WebhookService();
        $service->create([
            'name' => $body['name'] ?? '',
            'url' => $body['url'] ?? '',
            'is_active' => isset($body['is_active']) ? 1 : 0,
            'subscriptions' => $body['subscriptions'] ?? [],
        ]);

        return $response->withHeader('Location', '/admin/webhooks')->withStatus(302);
    }

    public function updateWebhook(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $body = $request->getParsedBody();

        $service = new WebhookService();
        $service->update((int) $route->getArgument('id'), [
            'name' => $body['name'] ?? '',
            'url' => $body['url'] ?? '',
            'is_active' => isset($body['is_active']) ? 1 : 0,
            'subscriptions' => $body['subscriptions'] ?? [],
        ]);

        return $response->withHeader('Location', '/admin/webhooks')->withStatus(302);
    }

    public function deleteWebhook(Request $request, Response $response): Response
    {
        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $service = new WebhookService();
        $service->delete((int) $route->getArgument('id'));
        return $response->withHeader('Location', '/admin/webhooks')->withStatus(302);
    }
}
