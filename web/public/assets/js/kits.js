/**
 * HyArena2 - Kits Page JavaScript
 * Fetches kit data with server-wide usage stats and renders stats table + kit cards
 */

document.addEventListener('DOMContentLoaded', () => {
    loadKits();
});

async function loadKits() {
    const loadingEl = document.getElementById('kits-loading');
    const contentEl = document.getElementById('kits-content');

    try {
        const response = await fetch('/api/kits');
        const data = await response.json();

        if (data.success) {
            const kits = data.data.kits || [];
            renderStatsTable(kits);
            renderKitCards(kits);
            loadingEl.style.display = 'none';
            contentEl.style.display = 'block';
        } else {
            loadingEl.innerHTML = '<div class="no-data-message"><p>Failed to load kit data.</p></div>';
        }
    } catch (e) {
        console.error('Failed to load kits:', e);
        loadingEl.innerHTML = '<div class="no-data-message"><p>Failed to load kit data.</p></div>';
    }
}

function renderStatsTable(kits) {
    const container = document.getElementById('kits-stats-container');
    if (!container) return;

    if (kits.length === 0) {
        container.innerHTML = '<div class="no-data-message"><p>No kit data available yet.</p></div>';
        return;
    }

    const rows = kits.map(kit => {
        const pickRate = parseFloat(kit.pick_rate || 0);
        const winRate = parseFloat(kit.avg_win_rate || 0);
        const avgKd = parseFloat(kit.avg_pvp_kd || 0);

        return `
            <tr>
                <td class="col-kit">${escapeHtml(kit.display_name)}</td>
                <td class="col-stat">${formatNumber(parseInt(kit.total_matches) || 0)}</td>
                <td class="col-stat col-usage">
                    <div class="usage-bar-wrapper">
                        <div class="usage-bar-fill" style="width: ${pickRate}%"></div>
                        <span class="usage-bar-text">${pickRate.toFixed(1)}%</span>
                    </div>
                </td>
                <td class="col-stat">${formatNumber(parseInt(kit.unique_players) || 0)}</td>
                <td class="col-stat highlight">${winRate.toFixed(1)}%</td>
                <td class="col-stat highlight">${avgKd.toFixed(2)}</td>
            </tr>
        `;
    }).join('');

    container.innerHTML = `
        <div class="kit-stats-table-wrapper">
            <table class="kit-stats-table">
                <thead>
                    <tr>
                        <th class="col-kit">Kit</th>
                        <th class="col-stat">Matches</th>
                        <th class="col-stat col-usage">Pick Rate</th>
                        <th class="col-stat">Players</th>
                        <th class="col-stat">Win Rate</th>
                        <th class="col-stat">Avg K/D</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>
    `;
}

function renderKitCards(kits) {
    const container = document.getElementById('kits-grid-container');
    if (!container) return;

    if (kits.length === 0) {
        container.innerHTML = '<div class="no-data-message"><p>No kits available yet.</p></div>';
        return;
    }

    const cards = kits.map(kit => {
        const matches = parseInt(kit.total_matches) || 0;
        const winRate = parseFloat(kit.avg_win_rate || 0);

        return `
            <article class="kit-card">
                <div class="kit-icon default">
                    <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="2.5">
                        <path d="M32 8v40M24 16h16M20 48h24M28 48v8M36 48v8"/>
                    </svg>
                </div>
                <h3 class="kit-title">${escapeHtml(kit.display_name)}</h3>
                <p class="kit-desc">${escapeHtml(kit.description || '')}</p>
                <p class="kit-mini-stats">${formatNumber(matches)} matches &middot; ${winRate.toFixed(1)}% win rate</p>
            </article>
        `;
    }).join('');

    container.innerHTML = `<div class="kits-grid">${cards}</div>`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toLocaleString('en-US');
}
