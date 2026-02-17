<?php

namespace App\Service;

class ImageService
{
    private const UPLOAD_DIR = __DIR__ . '/../../public/uploads/news/';
    private const MAX_SIZE = 400;
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
}
