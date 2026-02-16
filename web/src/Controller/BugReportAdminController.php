<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Auth\AdminPermissions;
use App\Repository\BugReportRepository;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class BugReportAdminController
{
    public function __construct(private Twig $twig) {}

    public function list(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('bug_reports');

        $params = $request->getQueryParams();
        $status = $params['status'] ?? null;
        $page = max(1, (int) ($params['page'] ?? 1));
        $limit = 20;
        $offset = ($page - 1) * $limit;

        $repo = new BugReportRepository();
        $reports = $repo->getAll($status, $limit, $offset);
        $total = $repo->getCount($status);
        $statusCounts = $repo->getCountByStatus();

        return $this->twig->render($response, 'admin/bug-reports.twig', [
            'admin' => AdminAuth::getAdmin(),
            'reports' => $reports,
            'status_filter' => $status,
            'status_counts' => $statusCounts,
            'page' => $page,
            'total_pages' => max(1, ceil($total / $limit)),
            'total' => $total,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'bug_reports',
        ]);
    }

    public function detail(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('bug_reports');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $repo = new BugReportRepository();
        $report = $repo->findById($id);
        if (!$report) {
            return $response->withHeader('Location', '/admin/bug-reports')->withStatus(302);
        }

        return $this->twig->render($response, 'admin/bug-report-detail.twig', [
            'admin' => AdminAuth::getAdmin(),
            'report' => $report,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'bug_reports',
        ]);
    }

    public function updateStatus(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('bug_reports');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');
        $body = $request->getParsedBody();
        $admin = AdminAuth::getAdmin();

        $repo = new BugReportRepository();
        $repo->updateStatus(
            $id,
            $body['status'] ?? 'open',
            $body['admin_notes'] ?? null,
            $admin['id']
        );

        return $response->withHeader('Location', '/admin/bug-reports/' . $id)->withStatus(302);
    }

    public function delete(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('bug_reports');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $repo = new BugReportRepository();
        $repo->delete($id);

        return $response->withHeader('Location', '/admin/bug-reports')->withStatus(302);
    }
}
