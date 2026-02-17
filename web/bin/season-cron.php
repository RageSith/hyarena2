#!/usr/bin/env php
<?php

// Season Lifecycle Cron Script
//
// Automates season lifecycle:
// - Auto-activates draft seasons when starts_at passes
// - Auto-ends active seasons when ends_at passes (freezes rankings)
// - Spawns next iteration for recurring seasons
//
// Only logs when actions are taken or on error.
// Log file: web/logs/season-cron.log (auto-created)
//
// Crontab entry (every 15 minutes):
//   */15 * * * * php /path/to/web/bin/season-cron.php

require __DIR__ . '/../vendor/autoload.php';

$logDir = __DIR__ . '/../logs';
if (!is_dir($logDir)) {
    mkdir($logDir, 0775, true);
}
$logFile = $logDir . '/season-cron.log';

function log_line(string $message): void
{
    global $logFile;
    $line = '[' . date('Y-m-d H:i:s') . '] ' . $message . "\n";
    file_put_contents($logFile, $line, FILE_APPEND | LOCK_EX);
}

try {
    $service = new App\Service\SeasonService();
    $result = $service->runCronCycle();

    $actions = $result['actions'];
    if (empty($actions)) {
        exit(0);
    }

    $c = $result['counts'];
    $summary = [];
    if ($c['activated'] > 0) $summary[] = "{$c['activated']} activated";
    if ($c['ended'] > 0) $summary[] = "{$c['ended']} ended";
    if ($c['spawned'] > 0) $summary[] = "{$c['spawned']} spawned";
    if ($c['stopped'] > 0) $summary[] = "{$c['stopped']} stopped";

    log_line(implode(', ', $summary));
    foreach ($actions as $action) {
        log_line('  > ' . $action);
    }

    exit(0);
} catch (\Throwable $e) {
    log_line('FAILED: ' . $e->getMessage());
    log_line($e->getTraceAsString());
    exit(1);
}
