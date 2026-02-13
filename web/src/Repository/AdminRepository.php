<?php

namespace App\Repository;

use App\Database;

class AdminRepository
{
    public function getDashboardStats(): array
    {
        $db = Database::getConnection();

        $players = (int) $db->query('SELECT COUNT(*) FROM players')->fetchColumn();
        $matches = (int) $db->query('SELECT COUNT(*) FROM matches')->fetchColumn();
        $arenas = (int) $db->query('SELECT COUNT(*) FROM arenas')->fetchColumn();
        $kits = (int) $db->query('SELECT COUNT(*) FROM kits')->fetchColumn();
        $linkedAccounts = (int) $db->query('SELECT COUNT(*) FROM linked_accounts WHERE player_uuid IS NOT NULL')->fetchColumn();
        $matchesToday = (int) $db->query("SELECT COUNT(*) FROM matches WHERE DATE(ended_at) = CURDATE()")->fetchColumn();

        return [
            'total_players' => $players,
            'total_matches' => $matches,
            'total_arenas' => $arenas,
            'total_kits' => $kits,
            'linked_accounts' => $linkedAccounts,
            'matches_today' => $matchesToday,
        ];
    }
}
