/**
 * HyArena2 - Leaderboard Page JavaScript
 * Handles arena tabs, sorting, and pagination
 */

let currentArena = 'global';
let currentSort = 'pvp_kills';
let sortDirection = 'desc';
let currentPage = 1;
let totalPages = 0;
let currentGameMode = null;
const ENTRIES_PER_PAGE = 25;

// Cache arena game modes from tab loading
const arenaGameModes = {};

// Column definitions per mode
const defaultHeaders = [
    { label: 'Wins', sort: 'matches_won' },
    { label: 'PvP Kills', sort: 'pvp_kills' },
    { label: 'PvP Deaths', sort: 'pvp_deaths' },
    { label: 'PvP K/D', sort: 'pvp_kd_ratio' },
    { label: 'PvE Kills', sort: 'pve_kills' },
    { label: 'PvE Deaths', sort: null },
    { label: 'Win Rate', sort: 'win_rate' },
];

const waveDefenseHeaders = [
    { label: 'Best Wave', sort: 'best_waves_survived' },
    { label: 'PvE Kills', sort: 'pve_kills' },
    { label: 'PvE Deaths', sort: null },
    { label: 'PvP Kills', sort: 'pvp_kills' },
    { label: 'PvP Deaths', sort: 'pvp_deaths' },
    { label: 'PvP K/D', sort: 'pvp_kd_ratio' },
    { label: 'Games', sort: 'matches_played' },
];

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    loadArenas();
    loadLeaderboardData();
    initPagination();
});

// ==========================================
// Tabs
// ==========================================
function initTabs() {
    const globalTab = document.querySelector('.tab-btn[data-arena="global"]');
    if (globalTab) {
        globalTab.addEventListener('click', () => selectArena('global'));
    }
}

async function loadArenas() {
    const tabsContainer = document.getElementById('leaderboard-tabs');
    if (!tabsContainer) return;

    try {
        const response = await fetch('/api/arenas');
        const data = await response.json();

        if (data.success && data.data.arenas && data.data.arenas.length > 0) {
            data.data.arenas.forEach(arena => {
                if (tabsContainer.querySelector(`[data-arena="${arena.id}"]`)) return;

                arenaGameModes[arena.id] = arena.game_mode;

                const btn = document.createElement('button');
                btn.className = 'tab-btn';
                btn.dataset.arena = arena.id;
                btn.textContent = arena.display_name;
                btn.addEventListener('click', () => selectArena(arena.id));
                tabsContainer.appendChild(btn);
            });
        }
    } catch (e) {
        console.log('Failed to load arenas:', e.message);
    }
}

function selectArena(arenaId) {
    currentArena = arenaId;
    currentPage = 1;

    // Reset sort to a sensible default for the mode
    const gameMode = arenaId === 'global' ? null : arenaGameModes[arenaId];
    if (gameMode === 'wave_defense') {
        currentSort = 'best_waves_survived';
    } else {
        currentSort = 'pvp_kills';
    }
    sortDirection = 'desc';

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.arena === arenaId);
    });

    loadLeaderboardData();
}

// ==========================================
// Load Data
// ==========================================
async function loadLeaderboardData() {
    const tbody = document.getElementById('leaderboard-body');
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="9">
                <div class="table-loading">
                    <div class="spinner"></div>
                    <span>Loading leaderboard...</span>
                </div>
            </td>
        </tr>
    `;

    try {
        const arena = currentArena === 'global' ? 'global' : currentArena;
        const response = await fetch(
            `/api/leaderboard?arena=${encodeURIComponent(arena)}&sort=${currentSort}&order=${sortDirection}&page=${currentPage}&per_page=${ENTRIES_PER_PAGE}`
        );
        const data = await response.json();

        if (data.success) {
            const entries = data.data.entries;
            totalPages = data.data.total_pages;
            currentGameMode = data.data.game_mode || null;

            // Update headers based on game mode
            updateTableHeaders();

            const summaryPlayers = document.getElementById('summary-players');
            if (summaryPlayers) {
                summaryPlayers.textContent = data.data.total.toLocaleString('en-US');
            }

            if (entries.length > 0) {
                renderLeaderboard(entries);
            } else {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="9">
                            <div class="table-empty">
                                <p>No players found for this arena yet.</p>
                            </div>
                        </td>
                    </tr>
                `;
            }

            updatePagination();
        }
    } catch (e) {
        console.log('Failed to load leaderboard:', e.message);
        tbody.innerHTML = `
            <tr>
                <td colspan="9">
                    <div class="table-error"><p>Failed to load leaderboard</p></div>
                </td>
            </tr>
        `;
    }
}

function isWaveDefense() {
    return currentGameMode === 'wave_defense';
}

function updateTableHeaders() {
    const headers = isWaveDefense() ? waveDefenseHeaders : defaultHeaders;
    const headerRow = document.querySelector('.leaderboard-table thead tr');
    if (!headerRow) return;

    // Keep rank + player columns, rebuild the stat columns
    const rankTh = headerRow.querySelector('.col-rank');
    const playerTh = headerRow.querySelector('.col-player');

    headerRow.innerHTML = '';
    headerRow.appendChild(rankTh);
    headerRow.appendChild(playerTh);

    headers.forEach(h => {
        const th = document.createElement('th');
        th.className = 'col-stat';
        th.textContent = h.label;
        if (h.sort) {
            th.classList.add('sortable');
            th.dataset.sort = h.sort;
            if (h.sort === currentSort) {
                th.classList.add('active', sortDirection);
            }
            th.addEventListener('click', () => {
                if (currentSort === h.sort) {
                    sortDirection = sortDirection === 'desc' ? 'asc' : 'desc';
                } else {
                    currentSort = h.sort;
                    sortDirection = 'desc';
                }
                headerRow.querySelectorAll('.sortable').forEach(el => {
                    el.classList.remove('active', 'asc', 'desc');
                });
                th.classList.add('active', sortDirection);
                currentPage = 1;
                loadLeaderboardData();
            });
        }
        headerRow.appendChild(th);
    });
}

function renderLeaderboard(entries) {
    const tbody = document.getElementById('leaderboard-body');
    const waveMode = isWaveDefense();

    tbody.innerHTML = entries.map(entry => {
        const rank = entry.rank_position;
        const rankClass = rank <= 3 ? `rank-${rank}` : '';
        const pvpKd = parseFloat(entry.pvp_kd_ratio || 0).toFixed(2);

        let statCells;
        if (waveMode) {
            const bestWave = entry.best_waves_survived != null ? entry.best_waves_survived : '-';
            statCells = `
                <td class="col-stat best-wave">${bestWave}</td>
                <td class="col-stat">${formatNumber(entry.pve_kills || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pve_deaths || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pvp_kills || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pvp_deaths || 0)}</td>
                <td class="col-stat kd-ratio">${pvpKd}</td>
                <td class="col-stat">${formatNumber(entry.matches_played || 0)}</td>
            `;
        } else {
            const winRate = parseFloat(entry.win_rate || 0).toFixed(1);
            statCells = `
                <td class="col-stat">${formatNumber(entry.matches_won || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pvp_kills || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pvp_deaths || 0)}</td>
                <td class="col-stat kd-ratio">${pvpKd}</td>
                <td class="col-stat">${formatNumber(entry.pve_kills || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pve_deaths || 0)}</td>
                <td class="col-stat win-rate">${winRate}%</td>
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
    document.getElementById('prev-page')?.addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            loadLeaderboardData();
        }
    });

    document.getElementById('next-page')?.addEventListener('click', () => {
        if (currentPage < totalPages) {
            currentPage++;
            loadLeaderboardData();
        }
    });
}

function updatePagination() {
    const prevBtn = document.getElementById('prev-page');
    const nextBtn = document.getElementById('next-page');
    const info = document.getElementById('pagination-info');

    if (prevBtn) prevBtn.disabled = currentPage <= 1;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages;
    if (info) info.textContent = `Page ${currentPage} of ${Math.max(totalPages, 1)}`;
}

// ==========================================
// Utilities
// ==========================================
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
