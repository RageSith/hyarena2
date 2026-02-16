/**
 * HyArena2 - Season Detail Page JavaScript
 * Displays season info and leaderboard (live or frozen)
 */

let currentPage = 1;
let totalPages = 0;
let seasonSlug = '';
let seasonData = null;

document.addEventListener('DOMContentLoaded', () => {
    seasonSlug = document.getElementById('season-slug')?.dataset.slug;
    if (!seasonSlug) {
        showError();
        return;
    }

    loadSeasonDetail();
    initPagination();
});

// ==========================================
// Load Season
// ==========================================
async function loadSeasonDetail() {
    try {
        const response = await fetch(`/api/seasons/${encodeURIComponent(seasonSlug)}`);
        const data = await response.json();

        if (data.success && data.data.season) {
            seasonData = data.data.season;
            displaySeasonHeader(seasonData);
            loadLeaderboard();

            document.getElementById('season-loading').style.display = 'none';
            document.getElementById('season-content').style.display = 'block';
            document.title = `${seasonData.name} | HyArena`;
        } else {
            showError();
        }
    } catch (e) {
        console.error('Failed to load season:', e);
        showError();
    }
}

function showError() {
    document.getElementById('season-loading').style.display = 'none';
    document.getElementById('season-error').style.display = 'flex';
}

// ==========================================
// Season Header
// ==========================================
function displaySeasonHeader(season) {
    document.getElementById('season-name').textContent = season.name;

    const statusEl = document.getElementById('season-status');
    statusEl.textContent = season.status.charAt(0).toUpperCase() + season.status.slice(1);
    statusEl.className = `season-status-badge ${season.status}`;

    const descEl = document.getElementById('season-description');
    if (season.description) {
        descEl.textContent = season.description;
        descEl.style.display = 'block';
    } else {
        descEl.style.display = 'none';
    }

    const startDate = new Date(season.starts_at).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
    const endDate = new Date(season.ends_at).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
    document.getElementById('season-dates').textContent = `${startDate} - ${endDate}`;
    document.getElementById('season-participants').textContent = `${season.participant_count || 0} participants`;
    document.getElementById('season-ranking-mode').textContent = formatRankingMode(season.ranking_mode);

    // Countdown for active seasons
    const countdownEl = document.getElementById('season-countdown');
    if (season.status === 'active') {
        const endsAt = new Date(season.ends_at);
        const now = new Date();
        const diff = endsAt - now;
        if (diff > 0) {
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            countdownEl.textContent = `${days}d ${hours}h remaining`;
        } else {
            countdownEl.textContent = 'Season ending soon';
        }
    } else if (season.status === 'ended' || season.status === 'archived') {
        countdownEl.textContent = 'Season has ended - Final standings';
    } else {
        countdownEl.style.display = 'none';
    }

    // Update table headers based on ranking mode
    updateTableHeaders(season.ranking_mode);
}

function updateTableHeaders(rankingMode) {
    const headerRow = document.getElementById('season-table-header');
    if (!headerRow) return;

    let headers;
    if (rankingMode === 'points') {
        headers = ['Rank', 'Player', 'Points', 'Wins', 'PvP Kills', 'PvP K/D', 'Games'];
    } else {
        headers = ['Rank', 'Player', 'Wins', 'PvP Kills', 'PvP K/D', 'Win Rate', 'Games'];
    }

    headerRow.innerHTML = headers.map((h, i) => {
        if (i === 0) return `<th class="col-rank">${h}</th>`;
        if (i === 1) return `<th class="col-player">${h}</th>`;
        return `<th class="col-stat">${h}</th>`;
    }).join('');
}

// ==========================================
// Leaderboard
// ==========================================
async function loadLeaderboard() {
    const tbody = document.getElementById('season-leaderboard-body');
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="7">
                <div class="table-loading">
                    <div class="spinner"></div>
                    <span>Loading rankings...</span>
                </div>
            </td>
        </tr>
    `;

    try {
        const response = await fetch(
            `/api/seasons/${encodeURIComponent(seasonSlug)}/leaderboard?page=${currentPage}&per_page=25`
        );
        const data = await response.json();

        if (data.success) {
            totalPages = data.data.total_pages;
            const entries = data.data.entries;

            if (entries.length > 0) {
                renderLeaderboard(entries, data.data.ranking_mode);
            } else {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="7">
                            <div class="table-empty">
                                <p>No ranked players yet${data.data.min_matches > 0 ? ` (minimum ${data.data.min_matches} matches required)` : ''}.</p>
                            </div>
                        </td>
                    </tr>
                `;
            }

            updatePagination();
        }
    } catch (e) {
        console.error('Failed to load season leaderboard:', e);
        tbody.innerHTML = `
            <tr>
                <td colspan="7">
                    <div class="table-error"><p>Failed to load rankings</p></div>
                </td>
            </tr>
        `;
    }
}

function renderLeaderboard(entries, rankingMode) {
    const tbody = document.getElementById('season-leaderboard-body');
    const isPoints = rankingMode === 'points';

    tbody.innerHTML = entries.map(entry => {
        const rank = entry.rank_position;
        const rankClass = rank <= 3 ? `rank-${rank}` : '';
        const pvpKd = parseFloat(entry.pvp_kd_ratio || 0).toFixed(2);
        const winRate = parseFloat(entry.win_rate || 0).toFixed(1);
        const points = parseFloat(entry.ranking_points || 0).toFixed(1);

        let statCells;
        if (isPoints) {
            statCells = `
                <td class="col-stat season-points">${points}</td>
                <td class="col-stat">${formatNumber(entry.matches_won || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pvp_kills || 0)}</td>
                <td class="col-stat">${pvpKd}</td>
                <td class="col-stat">${formatNumber(entry.matches_played || 0)}</td>
            `;
        } else {
            statCells = `
                <td class="col-stat">${formatNumber(entry.matches_won || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pvp_kills || 0)}</td>
                <td class="col-stat">${pvpKd}</td>
                <td class="col-stat">${winRate}%</td>
                <td class="col-stat">${formatNumber(entry.matches_played || 0)}</td>
            `;
        }

        return `
            <tr class="leaderboard-row ${rankClass}">
                <td class="col-rank">
                    <span class="rank-badge ${rankClass}">${rank}</span>
                </td>
                <td class="col-player">
                    <a href="/player/${encodeURIComponent(entry.username)}" class="player-name player-link">${escapeHtml(entry.username)}</a>
                </td>
                ${statCells}
            </tr>
        `;
    }).join('');
}

// ==========================================
// Pagination
// ==========================================
function initPagination() {
    document.getElementById('season-prev')?.addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            loadLeaderboard();
        }
    });

    document.getElementById('season-next')?.addEventListener('click', () => {
        if (currentPage < totalPages) {
            currentPage++;
            loadLeaderboard();
        }
    });
}

function updatePagination() {
    const paginationEl = document.getElementById('season-pagination');
    const prevBtn = document.getElementById('season-prev');
    const nextBtn = document.getElementById('season-next');
    const info = document.getElementById('season-pagination-info');

    if (totalPages > 1) {
        paginationEl.style.display = 'flex';
        prevBtn.disabled = currentPage <= 1;
        nextBtn.disabled = currentPage >= totalPages;
        info.textContent = `Page ${currentPage} of ${totalPages}`;
    } else {
        paginationEl.style.display = 'none';
    }
}

// ==========================================
// Utilities
// ==========================================
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
