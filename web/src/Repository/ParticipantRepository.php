<?php

namespace App\Repository;

use App\Database;

class ParticipantRepository
{
    public function create(array $data): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO match_participants
                (match_id, player_uuid, is_bot, bot_name, bot_difficulty, kit_id,
                 pvp_kills, pvp_deaths, pve_kills, pve_deaths,
                 damage_dealt, damage_taken, is_winner, waves_survived, json_data, finish_time_ms)
            VALUES
                (:match_id, :player_uuid, :is_bot, :bot_name, :bot_difficulty, :kit_id,
                 :pvp_kills, :pvp_deaths, :pve_kills, :pve_deaths,
                 :damage_dealt, :damage_taken, :is_winner, :waves_survived, :json_data, :finish_time_ms)
        ');
        $stmt->execute([
            'match_id' => $data['match_id'],
            'player_uuid' => $data['player_uuid'] ?? null,
            'is_bot' => $data['is_bot'] ? 1 : 0,
            'bot_name' => $data['bot_name'] ?? null,
            'bot_difficulty' => $data['bot_difficulty'] ?? null,
            'kit_id' => $data['kit_id'] ?? null,
            'pvp_kills' => $data['pvp_kills'] ?? 0,
            'pvp_deaths' => $data['pvp_deaths'] ?? 0,
            'pve_kills' => $data['pve_kills'] ?? 0,
            'pve_deaths' => $data['pve_deaths'] ?? 0,
            'damage_dealt' => $data['damage_dealt'] ?? 0,
            'damage_taken' => $data['damage_taken'] ?? 0,
            'is_winner' => $data['is_winner'] ? 1 : 0,
            'waves_survived' => $data['waves_survived'] ?? null,
            'json_data' => $data['json_data'] ?? null,
            'finish_time_ms' => $data['finish_time_ms'] ?? null,
        ]);
    }
}
