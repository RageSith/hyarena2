<?php

namespace App\Service;

class ImageService
{
    private const UPLOAD_DIR = __DIR__ . '/../../public/uploads/news/';
    private const ARENA_UPLOAD_DIR = __DIR__ . '/../../public/uploads/arenas/';
    private const TEAM_UPLOAD_DIR = __DIR__ . '/../../public/uploads/team/';
    private const ARENA_FONT_DIR = __DIR__ . '/../../public/assets/fonts/';
    private const MAX_SIZE = 400;
    private const ARENA_WIDTH = 640;
    private const ARENA_HEIGHT = 478;
    private const QUALITY = 85;
    private const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

    public static function processNewsImage(array $uploadedFile): ?string
    {
        if (empty($uploadedFile['tmp_name']) || $uploadedFile['error'] !== UPLOAD_ERR_OK) {
            return null;
        }

        $mime = mime_content_type($uploadedFile['tmp_name']);
        if (!in_array($mime, self::ALLOWED_TYPES, true)) {
            return null;
        }

        $source = match ($mime) {
            'image/jpeg' => imagecreatefromjpeg($uploadedFile['tmp_name']),
            'image/png' => imagecreatefrompng($uploadedFile['tmp_name']),
            'image/gif' => imagecreatefromgif($uploadedFile['tmp_name']),
            'image/webp' => imagecreatefromwebp($uploadedFile['tmp_name']),
            default => null,
        };

        if (!$source) {
            return null;
        }

        $origW = imagesx($source);
        $origH = imagesy($source);

        // Center-crop to square
        $cropSize = min($origW, $origH);
        $cropX = (int) (($origW - $cropSize) / 2);
        $cropY = (int) (($origH - $cropSize) / 2);

        $cropped = imagecrop($source, [
            'x' => $cropX,
            'y' => $cropY,
            'width' => $cropSize,
            'height' => $cropSize,
        ]);
        imagedestroy($source);

        if (!$cropped) {
            return null;
        }

        // Resize to 400x400
        $resized = imagecreatetruecolor(self::MAX_SIZE, self::MAX_SIZE);
        imagecopyresampled($resized, $cropped, 0, 0, 0, 0, self::MAX_SIZE, self::MAX_SIZE, $cropSize, $cropSize);
        imagedestroy($cropped);

        // Save as webp
        $filename = time() . '_' . bin2hex(random_bytes(8)) . '.webp';
        $path = self::UPLOAD_DIR . $filename;

        if (!is_dir(self::UPLOAD_DIR)) {
            mkdir(self::UPLOAD_DIR, 0755, true);
        }

        imagewebp($resized, $path, self::QUALITY);
        imagedestroy($resized);

        return $filename;
    }

    public static function deleteNewsImage(string $filename): void
    {
        $path = self::UPLOAD_DIR . $filename;
        if (file_exists($path)) {
            unlink($path);
        }
    }

    public static function processArenaImage(array $uploadedFile, string $caption): ?string
    {
        if (empty($uploadedFile['tmp_name']) || $uploadedFile['error'] !== UPLOAD_ERR_OK) {
            return null;
        }

        $mime = mime_content_type($uploadedFile['tmp_name']);
        if (!in_array($mime, self::ALLOWED_TYPES, true)) {
            return null;
        }

        $source = match ($mime) {
            'image/jpeg' => imagecreatefromjpeg($uploadedFile['tmp_name']),
            'image/png' => imagecreatefrompng($uploadedFile['tmp_name']),
            'image/gif' => imagecreatefromgif($uploadedFile['tmp_name']),
            'image/webp' => imagecreatefromwebp($uploadedFile['tmp_name']),
            default => null,
        };

        if (!$source) {
            return null;
        }

        $origW = imagesx($source);
        $origH = imagesy($source);

        // Center-crop to 640:478 aspect ratio
        $targetRatio = self::ARENA_WIDTH / self::ARENA_HEIGHT;
        $sourceRatio = $origW / $origH;

        if ($sourceRatio > $targetRatio) {
            $cropH = $origH;
            $cropW = (int) ($origH * $targetRatio);
            $cropX = (int) (($origW - $cropW) / 2);
            $cropY = 0;
        } else {
            $cropW = $origW;
            $cropH = (int) ($origW / $targetRatio);
            $cropX = 0;
            $cropY = (int) (($origH - $cropH) / 2);
        }

        $cropped = imagecrop($source, [
            'x' => $cropX,
            'y' => $cropY,
            'width' => $cropW,
            'height' => $cropH,
        ]);
        imagedestroy($source);

        if (!$cropped) {
            return null;
        }

        // Resize to target dimensions
        $resized = imagecreatetruecolor(self::ARENA_WIDTH, self::ARENA_HEIGHT);
        imagecopyresampled($resized, $cropped, 0, 0, 0, 0, self::ARENA_WIDTH, self::ARENA_HEIGHT, $cropW, $cropH);
        imagedestroy($cropped);

        // Overlay caption text
        self::overlayCaption($resized, $caption);

        // Save as webp
        $filename = time() . '_' . bin2hex(random_bytes(8)) . '.webp';
        $path = self::ARENA_UPLOAD_DIR . $filename;

        if (!is_dir(self::ARENA_UPLOAD_DIR)) {
            mkdir(self::ARENA_UPLOAD_DIR, 0755, true);
        }

        imagewebp($resized, $path, self::QUALITY);
        imagedestroy($resized);

        return $filename;
    }

    private static function overlayCaption(\GdImage $image, string $caption): void
    {
        $fonts = glob(self::ARENA_FONT_DIR . '*.ttf');
        if (empty($fonts)) {
            return;
        }

        $font = $fonts[array_rand($fonts)];
        $fontSize = 42;
        $caption = mb_strtoupper($caption);

        // Shrink font if text is too wide (leave 40px padding each side)
        $maxWidth = self::ARENA_WIDTH - 80;
        while ($fontSize > 18) {
            $bbox = imagettfbbox($fontSize, 0, $font, $caption);
            $textWidth = abs($bbox[2] - $bbox[0]);
            if ($textWidth <= $maxWidth) {
                break;
            }
            $fontSize -= 2;
        }

        $bbox = imagettfbbox($fontSize, 0, $font, $caption);
        $textWidth = abs($bbox[2] - $bbox[0]);
        $textHeight = abs($bbox[7] - $bbox[1]);
        $x = (int) ((self::ARENA_WIDTH - $textWidth) / 2);
        $y = (int) ((self::ARENA_HEIGHT + $textHeight) / 2);

        $black = imagecolorallocate($image, 0, 0, 0);
        $white = imagecolorallocate($image, 255, 255, 255);

        // Dark outline (draw text offset in 8 directions)
        $offsets = [[-2,-2],[-2,0],[-2,2],[0,-2],[0,2],[2,-2],[2,0],[2,2]];
        foreach ($offsets as [$dx, $dy]) {
            imagettftext($image, $fontSize, 0, $x + $dx, $y + $dy, $black, $font, $caption);
        }

        // White text on top
        imagettftext($image, $fontSize, 0, $x, $y, $white, $font, $caption);
    }

    public static function deleteArenaImage(string $filename): void
    {
        $path = self::ARENA_UPLOAD_DIR . $filename;
        if (file_exists($path)) {
            unlink($path);
        }
    }

    public static function processTeamImage(array $uploadedFile): ?string
    {
        if (empty($uploadedFile['tmp_name']) || $uploadedFile['error'] !== UPLOAD_ERR_OK) {
            return null;
        }

        $mime = mime_content_type($uploadedFile['tmp_name']);
        if (!in_array($mime, self::ALLOWED_TYPES, true)) {
            return null;
        }

        $source = match ($mime) {
            'image/jpeg' => imagecreatefromjpeg($uploadedFile['tmp_name']),
            'image/png' => imagecreatefrompng($uploadedFile['tmp_name']),
            'image/gif' => imagecreatefromgif($uploadedFile['tmp_name']),
            'image/webp' => imagecreatefromwebp($uploadedFile['tmp_name']),
            default => null,
        };

        if (!$source) {
            return null;
        }

        $origW = imagesx($source);
        $origH = imagesy($source);

        // Center-crop to square
        $cropSize = min($origW, $origH);
        $cropX = (int) (($origW - $cropSize) / 2);
        $cropY = (int) (($origH - $cropSize) / 2);

        $cropped = imagecrop($source, [
            'x' => $cropX,
            'y' => $cropY,
            'width' => $cropSize,
            'height' => $cropSize,
        ]);
        imagedestroy($source);

        if (!$cropped) {
            return null;
        }

        // Resize to 400x400
        $resized = imagecreatetruecolor(self::MAX_SIZE, self::MAX_SIZE);
        imagecopyresampled($resized, $cropped, 0, 0, 0, 0, self::MAX_SIZE, self::MAX_SIZE, $cropSize, $cropSize);
        imagedestroy($cropped);

        // Save as webp
        $filename = time() . '_' . bin2hex(random_bytes(8)) . '.webp';
        $path = self::TEAM_UPLOAD_DIR . $filename;

        if (!is_dir(self::TEAM_UPLOAD_DIR)) {
            mkdir(self::TEAM_UPLOAD_DIR, 0755, true);
        }

        imagewebp($resized, $path, self::QUALITY);
        imagedestroy($resized);

        return $filename;
    }

    public static function deleteTeamImage(string $filename): void
    {
        $path = self::TEAM_UPLOAD_DIR . $filename;
        if (file_exists($path)) {
            unlink($path);
        }
    }
}
