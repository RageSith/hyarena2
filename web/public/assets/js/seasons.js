/**
 * HyArena2 - Seasons Page JavaScript
 * Handles active and archived season listing
 */

document.addEventListener('DOMContentLoaded', () => {
    const activeGrid = document.getElementById('active-seasons-grid');
    const archiveGrid = document.getElementById('archive-seasons-grid');

    if (activeGrid) {
        loadActiveSeasons();
    }
    if (archiveGrid) {
        loadArchivedSeasons();
    }
});

// ==========================================
// Active Seasons
// ==========================================
async function loadActiveSeasons() {
    const loadingEl = document.getElementById('seasons-loading');
    const contentEl = document.getElementById('seasons-active');
    const archiveLink = document.getElementById('seasons-archive-link');

    try {
        const response = await fetch('/api/seasons');
        const data = await response.json();

        if (loadingEl) loadingEl.style.display = 'none';
        if (contentEl) contentEl.style.display = 'block';

        if (data.success && data.data.seasons && data.data.seasons.length > 0) {
            const grid = document.getElementById('active-seasons-grid');
            grid.innerHTML = data.data.seasons.map(season => renderSeasonCard(season)).join('');
        }

        // Always show archive link
        if (archiveLink) archiveLink.style.display = 'block';
    } catch (e) {
        console.error('Failed to load seasons:', e);
        if (loadingEl) loadingEl.style.display = 'none';
        if (contentEl) contentEl.style.display = 'block';
    }
}

// ==========================================
// Archived Seasons
// ==========================================
let archivePage = 1;
let archiveTotalPages = 0;

async function loadArchivedSeasons() {
    const loadingEl = document.getElementById('archive-loading');
    const contentEl = document.getElementById('archive-content');

    try {
        const response = await fetch(`/api/seasons/archive?page=${archivePage}&per_page=12`);
        const data = await response.json();

        if (loadingEl) loadingEl.style.display = 'none';
        if (contentEl) contentEl.style.display = 'block';

        if (data.success) {
            archiveTotalPages = data.data.total_pages;
            const grid = document.getElementById('archive-seasons-grid');

            if (data.data.seasons && data.data.seasons.length > 0) {
                grid.innerHTML = data.data.seasons.map(season => renderSeasonCard(season, true)).join('');
            } else {
                grid.innerHTML = '<div class="no-data-message"><p>No past seasons yet.</p></div>';
            }

            updateArchivePagination();
        }
    } catch (e) {
        console.error('Failed to load archived seasons:', e);
        if (loadingEl) loadingEl.style.display = 'none';
        if (contentEl) contentEl.style.display = 'block';
    }
}

function updateArchivePagination() {
    const paginationEl = document.getElementById('archive-pagination');
    const prevBtn = document.getElementById('archive-prev');
    const nextBtn = document.getElementById('archive-next');
    const info = document.getElementById('archive-pagination-info');

    if (archiveTotalPages > 1) {
        paginationEl.style.display = 'flex';
        prevBtn.disabled = archivePage <= 1;
        nextBtn.disabled = archivePage >= archiveTotalPages;
        info.textContent = `Page ${archivePage} of ${archiveTotalPages}`;

        prevBtn.onclick = () => { if (archivePage > 1) { archivePage--; loadArchivedSeasons(); } };
        nextBtn.onclick = () => { if (archivePage < archiveTotalPages) { archivePage++; loadArchivedSeasons(); } };
    }
}

// ==========================================
// Shared Card Renderer
// ==========================================
function renderSeasonCard(season, isEnded = false) {
    const startDate = new Date(season.starts_at).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    const endDate = new Date(season.ends_at).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    const participants = season.participant_count || 0;

    let statusClass = 'active';
    let statusText = 'Active';
    if (season.status === 'ended') { statusClass = 'ended'; statusText = 'Ended'; }
    else if (season.status === 'archived') { statusClass = 'archived'; statusText = 'Archived'; }

    const rankingLabel = formatRankingMode(season.ranking_mode);

    const isRecurring = season.recurrence && season.recurrence !== 'none';

    let countdownText = '';
    if (!isEnded && season.status === 'active') {
        const diff = new Date(season.ends_at) - new Date();
        if (diff > 0) {
            const label = isRecurring ? 'Resets in ' : '';
            const suffix = isRecurring ? '' : ' remaining';
            countdownText = label + formatTimeRemaining(diff) + suffix;
        } else {
            countdownText = isRecurring ? 'Resetting soon' : 'Ending soon';
        }
    }

    const isPrivate = season.visibility && season.visibility !== 'public';
    const privateClass = isPrivate ? ' season-card-private' : '';
    const privateBadge = isPrivate ? '<span class="season-card-badge-private">Private</span>' : '';

    return `
        <a href="/seasons/${encodeURIComponent(season.slug)}" class="season-card ${statusClass}${privateClass}">
            <div class="season-card-header">
                <h3 class="season-card-name">${escapeHtml(season.name)}${privateBadge}</h3>
                <div class="season-card-badges">
                    <span class="season-card-status ${statusClass}">${statusText}</span>
                    ${isRecurring ? `<span class="season-card-recurrence">${season.recurrence}</span>` : ''}
                </div>
            </div>
            <div class="season-card-meta">
                <span class="season-card-dates">${startDate} - ${endDate}</span>
                <span class="season-card-players">${participants} players</span>
            </div>
            ${season.description ? `<p class="season-card-desc">${escapeHtml(season.description)}</p>` : ''}
            <div class="season-card-footer">
                <span class="season-card-countdown">${countdownText}</span>
                <span class="season-card-ranking">${rankingLabel}</span>
            </div>
        </a>
    `;
}

function formatRankingMode(mode) {
    const labels = {
        'wins': 'Ranked by Wins',
        'win_rate': 'Ranked by Win Rate',
        'pvp_kills': 'Ranked by PvP Kills',
        'pvp_kd_ratio': 'Ranked by K/D Ratio',
        'points': 'Ranked by Points',
    };
    return labels[mode] || mode;
}

function formatTimeRemaining(ms) {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) {
        const remHours = hours % 24;
        return remHours > 0 ? `${days}d ${remHours}h` : `${days}d`;
    }
    if (hours > 0) {
        const remMin = minutes % 60;
        return remMin > 0 ? `${hours}h ${remMin}m` : `${hours}h`;
    }
    if (minutes > 0) {
        return `${minutes}m`;
    }
    return 'less than a minute';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
