<?php

namespace App;

class Config
{
    private static ?array $settings = null;

    public static function load(): array
    {
        if (self::$settings !== null) {
            return self::$settings;
        }

        $path = __DIR__ . '/../config/settings.php';
        if (!file_exists($path)) {
            throw new \RuntimeException('Config file not found. Copy config/settings.example.php to config/settings.php');
        }

        self::$settings = require $path;
        return self::$settings;
    }

    public static function get(string $key, mixed $default = null): mixed
    {
        $settings = self::load();
        $keys = explode('.', $key);
        $value = $settings;

        foreach ($keys as $k) {
            if (!is_array($value) || !array_key_exists($k, $value)) {
                return $default;
            }
            $value = $value[$k];
        }

        return $value;
    }
}
