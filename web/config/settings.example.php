<?php

return [
    'db' => [
        'host' => 'localhost',
        'port' => 3306,
        'name' => 'hyarena2',
        'user' => 'hyarena2',
        'pass' => '',
        'charset' => 'utf8mb4',
    ],
    'api' => [
        'key' => 'change-me-to-a-secure-random-string',
    ],
    'site' => [
        'name' => 'HyArena',
        'url' => 'https://hyarena.example.com',
        'debug' => false,
    ],
    'cors' => [
        'origin' => '*',
    ],
    'rate_limit' => [
        'requests_per_minute' => 60,
        'storage_path' => __DIR__ . '/../cache/rate_limit',
    ],
    'twig' => [
        'cache' => __DIR__ . '/../cache/twig',
    ],
    'admin' => [
        'session_lifetime' => 3600,
    ],
];
