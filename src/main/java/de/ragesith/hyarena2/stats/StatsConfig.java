package de.ragesith.hyarena2.stats;

/**
 * Configuration for stats tracking and web API integration.
 * Loaded from config/stats.json.
 */
public class StatsConfig {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:8080";
    private String apiKey = "";
    private boolean syncOnStartup = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isSyncOnStartup() {
        return syncOnStartup;
    }

    public void setSyncOnStartup(boolean syncOnStartup) {
        this.syncOnStartup = syncOnStartup;
    }
}
