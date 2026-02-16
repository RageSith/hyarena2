<?php

namespace App\Controller;

use App\Auth\AdminAuth;
use App\Auth\AdminPermissions;
use App\Repository\AdminUserRepository;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class AdminUserController
{
    public function __construct(private Twig $twig) {}

    public function list(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('admin_users');

        $repo = new AdminUserRepository();
        return $this->twig->render($response, 'admin/admin-users.twig', [
            'admin' => AdminAuth::getAdmin(),
            'users' => $repo->getAll(),
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'admin_users',
        ]);
    }

    public function createForm(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('admin_users');

        return $this->twig->render($response, 'admin/admin-user-form.twig', [
            'admin' => AdminAuth::getAdmin(),
            'user' => null,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'admin_users',
        ]);
    }

    public function create(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('admin_users');

        $body = $request->getParsedBody();
        $username = trim($body['username'] ?? '');
        $password = $body['password'] ?? '';
        $role = $body['role'] ?? 'moderator';

        // Validate
        if (empty($username) || empty($password)) {
            return $this->twig->render($response, 'admin/admin-user-form.twig', [
                'admin' => AdminAuth::getAdmin(),
                'user' => null,
                'error' => 'Username and password are required.',
                'csrf_token' => AdminAuth::generateCsrfToken(),
                'active_page' => 'admin_users',
            ]);
        }

        if (strlen($password) < 8) {
            return $this->twig->render($response, 'admin/admin-user-form.twig', [
                'admin' => AdminAuth::getAdmin(),
                'user' => null,
                'error' => 'Password must be at least 8 characters.',
                'csrf_token' => AdminAuth::generateCsrfToken(),
                'active_page' => 'admin_users',
            ]);
        }

        $repo = new AdminUserRepository();
        if ($repo->findByUsername($username)) {
            return $this->twig->render($response, 'admin/admin-user-form.twig', [
                'admin' => AdminAuth::getAdmin(),
                'user' => null,
                'error' => 'Username already exists.',
                'csrf_token' => AdminAuth::generateCsrfToken(),
                'active_page' => 'admin_users',
            ]);
        }

        if (!in_array($role, ['super_admin', 'admin', 'moderator'])) {
            $role = 'moderator';
        }

        $repo->create($username, $password, $role);
        return $response->withHeader('Location', '/admin/users')->withStatus(302);
    }

    public function editForm(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('admin_users');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');

        $repo = new AdminUserRepository();
        $user = $repo->findById($id);
        if (!$user) {
            return $response->withHeader('Location', '/admin/users')->withStatus(302);
        }

        return $this->twig->render($response, 'admin/admin-user-form.twig', [
            'admin' => AdminAuth::getAdmin(),
            'user' => $user,
            'csrf_token' => AdminAuth::generateCsrfToken(),
            'active_page' => 'admin_users',
        ]);
    }

    public function edit(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('admin_users');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');
        $body = $request->getParsedBody();
        $admin = AdminAuth::getAdmin();

        $repo = new AdminUserRepository();
        $user = $repo->findById($id);
        if (!$user) {
            return $response->withHeader('Location', '/admin/users')->withStatus(302);
        }

        $role = $body['role'] ?? $user['role'];
        $password = $body['password'] ?? '';

        if (!in_array($role, ['super_admin', 'admin', 'moderator'])) {
            $role = $user['role'];
        }

        // Prevent changing own role
        if ($id === $admin['id'] && $role !== $user['role']) {
            return $this->twig->render($response, 'admin/admin-user-form.twig', [
                'admin' => $admin,
                'user' => $user,
                'error' => 'You cannot change your own role.',
                'csrf_token' => AdminAuth::generateCsrfToken(),
                'active_page' => 'admin_users',
            ]);
        }

        // Prevent removing last super_admin
        if ($user['role'] === 'super_admin' && $role !== 'super_admin') {
            if ($repo->countSuperAdmins() <= 1) {
                return $this->twig->render($response, 'admin/admin-user-form.twig', [
                    'admin' => $admin,
                    'user' => $user,
                    'error' => 'Cannot demote the last super admin.',
                    'csrf_token' => AdminAuth::generateCsrfToken(),
                    'active_page' => 'admin_users',
                ]);
            }
        }

        // Validate password length if provided
        if (!empty($password) && strlen($password) < 8) {
            return $this->twig->render($response, 'admin/admin-user-form.twig', [
                'admin' => $admin,
                'user' => $user,
                'error' => 'Password must be at least 8 characters.',
                'csrf_token' => AdminAuth::generateCsrfToken(),
                'active_page' => 'admin_users',
            ]);
        }

        $repo->update($id, [
            'role' => $role,
            'password' => $password ?: null,
        ]);

        return $response->withHeader('Location', '/admin/users')->withStatus(302);
    }

    public function delete(Request $request, Response $response): Response
    {
        AdminPermissions::requirePermission('admin_users');

        $route = \Slim\Routing\RouteContext::fromRequest($request)->getRoute();
        $id = (int) $route->getArgument('id');
        $admin = AdminAuth::getAdmin();

        // Prevent self-deletion
        if ($id === $admin['id']) {
            return $response->withHeader('Location', '/admin/users')->withStatus(302);
        }

        $repo = new AdminUserRepository();
        $user = $repo->findById($id);

        // Prevent deleting last super_admin
        if ($user && $user['role'] === 'super_admin' && $repo->countSuperAdmins() <= 1) {
            return $response->withHeader('Location', '/admin/users')->withStatus(302);
        }

        $repo->delete($id);
        return $response->withHeader('Location', '/admin/users')->withStatus(302);
    }
}
