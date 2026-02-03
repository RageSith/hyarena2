package de.ragesith.hyarena2.ui.page;

/**
 * Interface for pages that have background tasks that need cleanup.
 * Implementing pages should stop all refresh tasks in shutdown().
 */
public interface CloseablePage {
    /**
     * Shuts down the page, stopping all background refresh tasks.
     * Called when the page is being replaced or the player disconnects.
     */
    void shutdown();
}
