<?php

namespace App\Controller;

use App\Service\LinkService;
use App\Repository\PlayerRepository;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Views\Twig;

class LinkController
{
    public function __construct(private Twig $twig) {}

    // ==========================================
    // API: Generate link code (called by plugin)
    // ==========================================

    public function generateCode(Request $request, Response $response): Response
    {
        $data = json_decode((string) $request->getBody(), true);
        $uuid = $data['uuid'] ?? '';

        if (empty($uuid)) {
            return $this->json($response, ['success' => false, 'error' => ['message' => 'Missing uuid', 'code' => 'VALIDATION_ERROR']], 400);
        }

        $service = new LinkService();
        $code = $service->generateCode($uuid);

        return $this->json($response, ['success' => true, 'data' => ['code' => $code]]);
    }

    // ==========================================
    // Web: Registration
    // ==========================================

    public function registerPage(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'auth/register.twig', [
            'active_page' => 'register',
        ]);
    }

    public function register(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();
        $email = trim($body['email'] ?? '');
        $password = $body['password'] ?? '';
        $passwordConfirm = $body['password_confirm'] ?? '';

        if (empty($email) || empty($password)) {
            return $this->twig->render($response, 'auth/register.twig', ['error' => 'All fields are required.']);
        }

        if ($password !== $passwordConfirm) {
            return $this->twig->render($response, 'auth/register.twig', ['error' => 'Passwords do not match.']);
        }

        if (strlen($password) < 8) {
            return $this->twig->render($response, 'auth/register.twig', ['error' => 'Password must be at least 8 characters.']);
        }

        $service = new LinkService();
        $result = $service->register($email, $password);

        if (!$result['success']) {
            return $this->twig->render($response, 'auth/register.twig', ['error' => $result['error']]);
        }

        // Auto-login
        $this->startPlayerSession($result['account_id'], $email);
        return $response->withHeader('Location', '/link')->withStatus(302);
    }

    // ==========================================
    // Web: Login
    // ==========================================

    public function loginPage(Request $request, Response $response): Response
    {
        return $this->twig->render($response, 'auth/login.twig', [
            'active_page' => 'login',
        ]);
    }

    public function login(Request $request, Response $response): Response
    {
        $body = $request->getParsedBody();
        $email = trim($body['email'] ?? '');
        $password = $body['password'] ?? '';

        $service = new LinkService();
        $result = $service->login($email, $password);

        if (!$result['success']) {
            return $this->twig->render($response, 'auth/login.twig', ['error' => $result['error']]);
        }

        $account = $result['account'];
        $this->startPlayerSession($account['id'], $account['email'], $account['player_uuid']);

        return $response->withHeader('Location', '/profile')->withStatus(302);
    }

    public function logout(Request $request, Response $response): Response
    {
        if (session_status() === PHP_SESSION_NONE) session_start();
        $_SESSION = [];

        // Clear the session cookie
        $params = session_get_cookie_params();
        setcookie(session_name(), '', time() - 3600, $params['path'], $params['domain'], $params['secure'], $params['httponly']);

        session_destroy();
        return $response->withHeader('Location', '/')->withStatus(302);
    }

    // ==========================================
    // Web: Link Account
    // ==========================================

    public function linkPage(Request $request, Response $response): Response
    {
        if (session_status() === PHP_SESSION_NONE) session_start();

        return $this->twig->render($response, 'auth/link.twig', [
            'active_page' => 'link',
            'is_linked' => !empty($_SESSION['player_uuid']),
        ]);
    }

    public function link(Request $request, Response $response): Response
    {
        if (session_status() === PHP_SESSION_NONE) session_start();

        $body = $request->getParsedBody();
        $code = trim($body['code'] ?? '');
        $accountId = $_SESSION['player_account_id'];

        $service = new LinkService();
        $result = $service->linkAccount($accountId, $code);

        if (!$result['success']) {
            return $this->twig->render($response, 'auth/link.twig', [
                'error' => $result['error'],
                'is_linked' => !empty($_SESSION['player_uuid']),
            ]);
        }

        $_SESSION['player_uuid'] = $result['player_uuid'];
        return $response->withHeader('Location', '/profile')->withStatus(302);
    }

    // ==========================================
    // Web: Profile
    // ==========================================

    public function profile(Request $request, Response $response): Response
    {
        if (session_status() === PHP_SESSION_NONE) session_start();

        $service = new LinkService();
        $account = $service->getAccount($_SESSION['player_account_id']);
        $player = null;

        if ($account && $account['player_uuid']) {
            $playerRepo = new PlayerRepository();
            $player = $playerRepo->findByUuid($account['player_uuid']);
        }

        return $this->twig->render($response, 'auth/profile.twig', [
            'active_page' => 'profile',
            'account' => $account,
            'player' => $player,
        ]);
    }

    // ==========================================
    // Helpers
    // ==========================================

    private function startPlayerSession(int $accountId, string $email, ?string $playerUuid = null): void
    {
        if (session_status() === PHP_SESSION_NONE) session_start();
        $_SESSION['player_account_id'] = $accountId;
        $_SESSION['player_email'] = $email;
        $_SESSION['player_uuid'] = $playerUuid;
    }

    private function json(Response $response, array $data, int $status = 200): Response
    {
        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json')->withStatus($status);
    }
}
