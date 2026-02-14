package de.ragesith.hyarena2.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Lightweight async HTTP client for the HyArena web API.
 * Uses JDK built-in java.net.http.HttpClient — no external dependencies.
 */
public class ApiClient {
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public ApiClient(String baseUrl, String apiKey) {
        // Strip trailing slash
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Sends an async POST request with JSON body.
     * Never blocks the calling thread. Errors are logged, never thrown.
     */
    public CompletableFuture<HttpResponse<String>> postAsync(String path, String jsonBody) {
        String url = baseUrl + path;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("X-API-Key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .exceptionally(ex -> {
                System.err.println("[ApiClient] POST " + path + " failed: " + ex.getMessage());
                return null;
            });
    }

    /**
     * Sends an async GET request.
     * Never blocks the calling thread. Errors are logged, never thrown.
     */
    public CompletableFuture<HttpResponse<String>> getAsync(String path) {
        String url = baseUrl + path;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .header("X-API-Key", apiKey)
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .exceptionally(ex -> {
                System.err.println("[ApiClient] GET " + path + " failed: " + ex.getMessage());
                return null;
            });
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
